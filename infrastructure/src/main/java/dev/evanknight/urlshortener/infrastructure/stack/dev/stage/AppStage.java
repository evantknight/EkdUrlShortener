package dev.evanknight.urlshortener.infrastructure.stack.dev.stage;

import dev.evanknight.urlshortener.infrastructure.stack.service.ServiceStack;
import lombok.Getter;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stage;
import software.amazon.awscdk.StageProps;
import software.constructs.Construct;

import static dev.evanknight.urlshortener.infrastructure.constant.StageNames.PROD_STAGE_NAME;
import static dev.evanknight.urlshortener.infrastructure.util.NameUtils.getStackName;

public class AppStage extends Stage {

    private static final String STACK_NAME = getStackName(ServiceStack.class);

    @Getter
    private final CfnOutput apiEndpoint;

    public AppStage(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public AppStage(final Construct scope, final String id, final StageProps props) {
        super(scope, id, props);

        final boolean addDns = PROD_STAGE_NAME.equals(props.getStageName());
        final ServiceStack serviceStack = new ServiceStack(this, STACK_NAME, addDns);
        apiEndpoint = serviceStack.getApiEndpoint();
    }
}
