package dev.evanknight.urlshortener.infrastructure;

import dev.evanknight.urlshortener.infrastructure.util.StackUtils;
import dev.evanknight.urlshortener.infrastructure.stack.service.ServiceStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.util.Arrays;

public class EkdUrlShortenerApp {
    public static void main(final String[] args) {
        final App app = new App();

        System.out.println("Args: " + Arrays.toString(args));

        final ServiceStack serviceStack = new ServiceStack(app,
                                                           StackUtils.getStackName(ServiceStack.class),
                                                           StackProps.builder()
                                                                   .env(buildEnvironment())
                                                                   .build());

        app.synth();
    }

    private static Environment buildEnvironment() {
        final String account = System.getenv("CDK_DEFAULT_ACCOUNT");
        final String region = System.getenv("CDK_DEFAULT_REGION");

        return Environment.builder()
                .account(account)
                .region(region)
                .build();
    }
}
