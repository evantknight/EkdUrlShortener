package dev.evanknight.urlshortener.infrastructure.stack.dev;

import com.google.common.collect.ImmutableMap;
import dev.evanknight.urlshortener.infrastructure.stack.dev.stage.AppStage;
import software.amazon.awscdk.App;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.StageProps;
import software.amazon.awscdk.pipelines.CodeBuildStep;
import software.amazon.awscdk.pipelines.CodePipeline;
import software.amazon.awscdk.pipelines.CodePipelineSource;
import software.amazon.awscdk.pipelines.IFileSetProducer;
import software.amazon.awscdk.pipelines.StageDeployment;
import software.amazon.awscdk.pipelines.Step;
import software.amazon.awscdk.services.codebuild.BuildSpec;
import software.amazon.awscdk.services.codebuild.Cache;
import software.amazon.awscdk.services.codecommit.Repository;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;

import java.util.List;
import java.util.Map;

import static dev.evanknight.urlshortener.constants.SysEnv.API_ENDPOINT;
import static dev.evanknight.urlshortener.infrastructure.constant.StageNames.PROD_STAGE_NAME;
import static dev.evanknight.urlshortener.infrastructure.constant.StageNames.TEST_STAGE_NAME;

public class DevStack extends Stack {

    // Repository constants
    private static final String REPOSITORY_NAME = "EkdUrlShortener";
    private static final String REPOSITORY_BRANCH = "mainline";

    // Pipeline constants
    private static final String PIPELINE_NAME = "UrlShortenerPipeline";
    private static final String SYNTH_STEP_NAME = "Synth";

    // Cache constants
    private static final List<String> CACHE_PATHS = List.of(
            "gradleHome/caches/**/*",
            "gradleHome/wrapper/**/*",
            "gradleHome/notifications/**/*"
    );
    private static final Map<String, ?> CACHE_SPEC_MAP = Map.of("cache",
                                                                Map.of("paths", CACHE_PATHS));

    // Test step constants
    private static final Map<String, ?> TEST_SPEC_MAP = ImmutableMap.<String, Object>builder()
            .putAll(CACHE_SPEC_MAP)
            .put("reports",
                 Map.of("junit-reports", Map.of(
                         "files", List.of("tests/build/test-results/test/*.xml"),
                         "file-format", "JUNITXML")))
            .build();


    public DevStack(final App scope, final String id) {
        this(scope, id, null);
    }

    public DevStack(final App scope, final String id, final StackProps props) {
        super (scope, id, props);

        final CodePipeline pipeline = getPipeline(props);
    }

    private CodePipeline getPipeline(final StackProps stackProps) {
        final CodePipelineSource source = getPipelineSource();
        final CodeBuildStep synthStep = getSynthStep(source);
        final CodePipeline pipeline = CodePipeline.Builder.create(this, PIPELINE_NAME)
                .pipelineName(PIPELINE_NAME)
                .synth(synthStep)
                .build();

        // Test stage
        final StageProps testStageProps = StageProps.builder()
                .stageName(TEST_STAGE_NAME)
                .env(stackProps.getEnv())
                .build();
        final AppStage testStage = new AppStage(this, "TestStage", testStageProps);
        final StageDeployment testDeployment = pipeline.addStage(testStage);
        testDeployment.addPost(getTestStep(TEST_STAGE_NAME, source, testStage.getApiEndpoint()));

        // Prod stage
        final StageProps prodStageProps = StageProps.builder()
                .stageName(PROD_STAGE_NAME)
                .env(stackProps.getEnv())
                .build();
        final AppStage prodStage = new AppStage(this, "ProdStage", prodStageProps);
        final StageDeployment prodDeployment = pipeline.addStage(prodStage);
        prodDeployment.addPost(getTestStep(PROD_STAGE_NAME, source, prodStage.getApiEndpoint()));

        return pipeline;
    }

    private CodePipelineSource getPipelineSource() {
        final Repository repository = Repository.Builder.create(this, REPOSITORY_NAME)
                .repositoryName(REPOSITORY_NAME)
                .build();
        return CodePipelineSource.codeCommit(repository, REPOSITORY_BRANCH);
    }

    private CodeBuildStep getSynthStep(final CodePipelineSource source) {
        final Bucket cacheBucket = Bucket.Builder.create(this, "BuildCacheBucket")
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        return CodeBuildStep.Builder.create(SYNTH_STEP_NAME)
                .input(source)
                .installCommands(List.of(
                        "npm install -g aws-cdk"
                ))
                .commands(List.of(
                        "chmod +x ./gradlew",
                        "cd infrastructure",
                        "cdk synth --app \"cd .. && ./gradlew --build-cache --gradle-user-home gradleHome/ run\""
                ))
                .rolePolicyStatements(List.of(PolicyStatement.Builder.create()
                                                      .actions(List.of("route53:ListHostedZonesByName"))
                                                      .resources(List.of("*"))
                                                      .build()))
                .primaryOutputDirectory("infrastructure/cdk.out")
                .cache(Cache.bucket(cacheBucket))
                .partialBuildSpec(BuildSpec.fromObject(CACHE_SPEC_MAP))
                .build();
    }

    private Step getTestStep(final String id,
                             final IFileSetProducer input,
                             final CfnOutput apiEndpoint) {
        final Bucket cacheBucket = Bucket.Builder.create(this, id + "-IntegrationTestCacheBucket")
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        return CodeBuildStep.Builder.create("IntegrationTests")
                .input(input)
                .envFromCfnOutputs(Map.of(API_ENDPOINT, apiEndpoint))
                .commands(List.of(
                        "chmod +x ./gradlew",
                        "./gradlew --build-cache --gradle-user-home gradleHome/ :tests:test"
                ))
                .partialBuildSpec(BuildSpec.fromObject(TEST_SPEC_MAP))
                .cache(Cache.bucket(cacheBucket))
                .build();
    }

}
