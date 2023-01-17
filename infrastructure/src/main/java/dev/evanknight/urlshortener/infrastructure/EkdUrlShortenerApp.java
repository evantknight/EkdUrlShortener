package dev.evanknight.urlshortener.infrastructure;

import dev.evanknight.urlshortener.infrastructure.stack.dev.DevStack;
import dev.evanknight.urlshortener.infrastructure.util.NameUtils;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class EkdUrlShortenerApp {
    public static void main(final String[] args) {
        final App app = new App();

        final DevStack devStack = new DevStack(app,
                                               NameUtils.getStackName(DevStack.class),
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
