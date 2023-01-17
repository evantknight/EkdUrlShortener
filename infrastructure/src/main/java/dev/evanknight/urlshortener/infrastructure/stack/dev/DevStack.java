package dev.evanknight.urlshortener.infrastructure.stack.dev;

import dev.evanknight.urlshortener.infrastructure.stack.dev.stage.AppStage;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Stage;
import software.amazon.awscdk.StageProps;
import software.amazon.awscdk.pipelines.CodeBuildStep;
import software.amazon.awscdk.pipelines.CodePipeline;
import software.amazon.awscdk.pipelines.CodePipelineSource;
import software.amazon.awscdk.services.codecommit.Repository;
import software.amazon.awscdk.services.iam.PolicyStatement;

import java.util.List;

import static dev.evanknight.urlshortener.infrastructure.constant.StageNames.PROD_STAGE_NAME;
import static dev.evanknight.urlshortener.infrastructure.constant.StageNames.TEST_STAGE_NAME;

public class DevStack extends Stack {

    // Repository constants
    private static final String REPOSITORY_NAME = "EkdUrlShortener";
    private static final String REPOSITORY_BRANCH = "mainline";

    // Pipeline constants
    private static final String PIPELINE_NAME = "UrlShortenerPipeline";
    private static final String SYNTH_STEP_NAME = "Synth";

    public DevStack(final App scope, final String id) {
        this(scope, id, null);
    }

    public DevStack(final App scope, final String id, final StackProps props) {
        super (scope, id, props);

        final CodePipeline pipeline = getPipeline(props);
    }

    private CodePipeline getPipeline(final StackProps stackProps) {
        final CodePipeline pipeline = CodePipeline.Builder.create(this, PIPELINE_NAME)
                .pipelineName(PIPELINE_NAME)
                .synth(getSynthStep())
                .build();

        // Test stage
        final StageProps testStageProps = StageProps.builder()
                .stageName(TEST_STAGE_NAME)
                .env(stackProps.getEnv())
                .build();
        final Stage testStage = new AppStage(this, "TestStage", testStageProps);
        pipeline.addStage(testStage);

        // Prod stage
        final StageProps prodStageProps = StageProps.builder()
                .stageName(PROD_STAGE_NAME)
                .env(stackProps.getEnv())
                .build();
        final Stage prodStage = new AppStage(this, "ProdStage", prodStageProps);
        pipeline.addStage(prodStage);

        return pipeline;
    }

    private CodeBuildStep getSynthStep() {
        final Repository repository = Repository.Builder.create(this, REPOSITORY_NAME)
                .repositoryName(REPOSITORY_NAME)
                .build();

        return CodeBuildStep.Builder.create(SYNTH_STEP_NAME)
                .input(CodePipelineSource.codeCommit(repository, REPOSITORY_BRANCH))
                .installCommands(List.of(
                        "npm install -g aws-cdk"
                ))
                .commands(List.of(
                        "chmod +x ./gradlew",
                        "cd infrastructure",
                        "cdk synth --app \"cd .. && ./gradlew run\""
                ))
                .rolePolicyStatements(List.of(PolicyStatement.Builder.create()
                                                      .actions(List.of("route53:ListHostedZonesByName"))
                                                      .resources(List.of("*"))
                                                      .build()))
                .primaryOutputDirectory("infrastructure/cdk.out")
                .build();
    }

}
