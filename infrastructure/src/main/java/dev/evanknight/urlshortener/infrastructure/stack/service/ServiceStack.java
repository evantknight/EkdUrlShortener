package dev.evanknight.urlshortener.infrastructure.stack.service;

import dev.evanknight.urlshortener.constants.DynamoDb;
import dev.evanknight.urlshortener.constants.Lambda;
import dev.evanknight.urlshortener.service.lambda.ReadLambda;
import dev.evanknight.urlshortener.service.lambda.WriteLambda;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigatewayv2.alpha.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.alpha.CorsHttpMethod;
import software.amazon.awscdk.services.apigatewayv2.alpha.CorsPreflightOptions;
import software.amazon.awscdk.services.apigatewayv2.alpha.DomainMappingOptions;
import software.amazon.awscdk.services.apigatewayv2.alpha.DomainName;
import software.amazon.awscdk.services.apigatewayv2.alpha.EndpointType;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpMethod;
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.HttpLambdaIntegration;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.lambda.CfnFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneProviderProps;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.ApiGatewayv2DomainProperties;

import java.util.List;
import java.util.Map;

public class ServiceStack extends Stack {

    // Application Constants
    private static final String SERVICE_NAME = "URL Shortener Service";
    private static final String ROOT_DOMAIN_NAME = "evanknight.dev";
    private static final String DOMAIN_NAME = "short." + ROOT_DOMAIN_NAME;

    // Table constants
    private static final String URL_TABLE_NAME = "UrlTable";
    private static final String DDB_ACTION_PREFIX = "dynamodb:";
    private static final String DDB_GET_ITEM_ACTION = DDB_ACTION_PREFIX + "GetItem";
    private static final String DDB_PUT_ITEM_ACTION = DDB_ACTION_PREFIX + "PutItem";

    // Lambda constants
    private static final String LAMBDA_ZIP_PATH = "../service/build/distributions/service-1.0-SNAPSHOT.zip";
    private static final String URL_READ_LAMBDA_NAME = "UrlReadLambda";
    private static final String URL_WRITE_LAMBDA_NAME = "UrlWriteLambda";
    private static final String READ_LAMBDA_HANDLER = ReadLambda.class.getName();
    private static final String WRITE_LAMBDA_HANDLER = WriteLambda.class.getName();

    // Certificate constants
    private static final String CERTIFICATE_NAME = "Certificate";
    private static final String ALTERNATIVE_NAME = "www." + DOMAIN_NAME;

    // ApiGateway constants
    private static final String DOMAIN_NAME_NAME = "DomainName";
    private static final String API_NAME = "UrlShortenerApi";
    private static final String API_PATH = "/{shortUrl}";
    private static final String API_URL = "https://" + DOMAIN_NAME;

    // Route53 Constants
    private static final String HOSTED_ZONE_NAME = "HostedZone";
    private static final String RECORD_NAME = "ApiRecord";

    public ServiceStack(final App scope, final String id) {
        this(scope, id, null);
    }

    public ServiceStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);

        // DDB URL Table
        final Table urlTable = getUrlDdbTable();

        // Read Lambda
        final Function readLambda = getReadLambda(urlTable);

        // Write Lambda
        final Function writeLambda = getWriteLambda(urlTable);

        // Route 53 (DNS)
        final IHostedZone hostedZone = getDns();

        // Certificate
        final Certificate certificate = getCertificate(hostedZone);

        // API Gateway
        buildApi(certificate, readLambda, writeLambda, hostedZone);
    }

    private Table getUrlDdbTable() {
        return Table.Builder.create(this, URL_TABLE_NAME)
                .partitionKey(Attribute.builder()
                        .name(DynamoDb.PRIMARY_KEY)
                        .type(AttributeType.STRING)
                        .build())
                .timeToLiveAttribute(DynamoDb.TTL_NAME)
                .build();
    }

    private Function getLambda(final String name,
                               final String handler,
                               final Map<String, String> envProps) {
        final Function function = Function.Builder.create(this, name)
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset(LAMBDA_ZIP_PATH))
                .functionName(name)
                .handler(handler)
                .timeout(Duration.seconds(30))
                .memorySize(1024)
                .environment(envProps)
                .logRetention(RetentionDays.ONE_DAY)
                .build();

        // Apply SnapStart
        // TODO: Move to L2 construct once this is merged: https://github.com/aws/aws-cdk/pull/23196
        final CfnFunction cfnFunction = (CfnFunction) function.getNode().getDefaultChild();
        cfnFunction.setSnapStart(CfnFunction.SnapStartProperty.builder()
                                         .applyOn("PublishedVersions")
                                         .build());

        return function;
    }

    private static Map<String, String> getLambdaProps(final Table table) {
        return Map.of(Lambda.DDB_TABLE_NAME, table.getTableName());
    }

    private Function getReadLambda(final Table table) {
        final Function readLambda = getLambda(URL_READ_LAMBDA_NAME,
                                              READ_LAMBDA_HANDLER,
                                              getLambdaProps(table));
        table.grant(readLambda, DDB_GET_ITEM_ACTION);
        return readLambda;
    }

    private Function getWriteLambda(final Table table) {
        final Function writeLambda = getLambda(URL_WRITE_LAMBDA_NAME,
                                               WRITE_LAMBDA_HANDLER,
                                               getLambdaProps(table));
        table.grant(writeLambda, DDB_GET_ITEM_ACTION, DDB_PUT_ITEM_ACTION);
        return writeLambda;
    }

    private IHostedZone getDns() {
        final HostedZoneProviderProps props = HostedZoneProviderProps.builder()
                .domainName(ROOT_DOMAIN_NAME)
                .build();
        return HostedZone.fromLookup(this, HOSTED_ZONE_NAME, props);
    }

    private Certificate getCertificate(final IHostedZone hostedZone) {
        return Certificate.Builder.create(this, CERTIFICATE_NAME)
                .domainName(DOMAIN_NAME)
                .subjectAlternativeNames(List.of(ALTERNATIVE_NAME))
                .certificateName(SERVICE_NAME)
                .validation(CertificateValidation.fromDns(hostedZone))
                .build();
    }

    private void buildApi(final Certificate certificate,
                          final Function readLambda,
                          final Function writeLambda,
                          final IHostedZone hostedZone) {

        // Build custom domain.
        final DomainName domainName = DomainName.Builder.create(this, DOMAIN_NAME_NAME)
                .domainName(DOMAIN_NAME)
                .certificate(certificate)
                .endpointType(EndpointType.REGIONAL)
                .build();

        // Create API.
        final HttpApi httpApi = HttpApi.Builder.create(this, API_NAME)
                .corsPreflight(CorsPreflightOptions.builder()
                                       .allowMethods(List.of(CorsHttpMethod.GET, CorsHttpMethod.POST))
                                       .allowOrigins(List.of(API_URL))
                                       .maxAge(Duration.days(7))
                                       .build())
                .defaultDomainMapping(DomainMappingOptions.builder()
                                              .domainName(domainName)
                                              .build())
                .build();

        // Add route and integrations.
        // Note: CurrentVersion is used as a workaround in order to use Lambda SnapStart. If we were to pass just
        // the Lambda, ApiGateway would use the wrong version.
        final HttpLambdaIntegration readLambdaIntegration = new HttpLambdaIntegration(URL_READ_LAMBDA_NAME, readLambda.getCurrentVersion());
        final HttpLambdaIntegration writeLambdaIntegration = new HttpLambdaIntegration(URL_WRITE_LAMBDA_NAME, writeLambda.getCurrentVersion());
        httpApi.addRoutes(AddRoutesOptions.builder()
                                  .path(API_PATH)
                                  .methods(List.of(HttpMethod.GET))
                                  .integration(readLambdaIntegration)
                                  .build());
        httpApi.addRoutes(AddRoutesOptions.builder()
                                  .path(API_PATH)
                                  .methods(List.of(HttpMethod.POST))
                                  .integration(writeLambdaIntegration)
                                  .build());

        // Add domain record to DNS.
        ARecord.Builder.create(this, RECORD_NAME)
                .zone(hostedZone)
                .recordName(DOMAIN_NAME)
                .target(RecordTarget.fromAlias(new ApiGatewayv2DomainProperties(domainName.getRegionalDomainName(),
                                                                                domainName.getRegionalHostedZoneId())))
                .build();
    }
}
