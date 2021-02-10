import dependencies.Config;
import dependencies.Emailer;
import dependencies.Logger;
import dependencies.Project;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;

@AllArgsConstructor
public class Pipeline {
    private final Config config;
    private final Emailer emailer;
    private final Logger log;
    private static final String SUCCESS = "success";

    public void run(Project project) {
        createProjectContext(project)
                .flatMap(this::runTests)
                .flatMap(this::deploy)
                .onFailure(exception -> log.error("An unexpected exception occurred"));
    }

    private Try<PipelineContext> createProjectContext(Project project) {
        return Try.of(() -> new PipelineContext(project))
                .peek(context -> { if (!context.isHasTests()) log.info("No tests"); });
    }

    private Try<PipelineContext> runTests(PipelineContext context) {
        return context.hasTests ?
                Try.of(() -> context.getProject().runTests())
                        .map(testsResult -> context.withTestsRanSuccessfully(testsResult.equals(SUCCESS)))
                        .peek(this::logTestResult) :
                Try.of(() -> context);
    }

    private void logTestResult(PipelineContext context) {
        if (context.isTestsRanSuccessfully()) log.info("Tests passed");
        else {
            log.error("Tests failed");
            sendEmail("Tests failed");
        }
    }

    private Try<PipelineContext> deploy(PipelineContext context) {
        return context.mustRunDeployment() ?
                Try.of(() -> context.getProject().deploy())
                        .map(deploymentResult -> context.withDeployedSuccessfully(deploymentResult.equals(SUCCESS)))
                        .peek(this::logDeploymentResult) :
                Try.of(() -> context);
    }

    private void logDeploymentResult(PipelineContext context) {
        if (context.isDeployedSuccessfully()) {
            log.info("Deployment successful");
            sendEmail("Deployment completed successfully");
        } else {
            log.error("Deployment failed");
            sendEmail("Deployment failed");
        }
    }

    private void sendEmail(String text) {
        if (config.sendEmailSummary()) {
            log.info("Sending email");
            emailer.send(text);
        } else {
            log.info("Email disabled");
        }
    }

    @Getter
    @AllArgsConstructor
    @With
    private static class PipelineContext {
        private final boolean hasTests;
        private final Project project;
        private boolean testsRanSuccessfully;
        private boolean deployedSuccessfully;

        public PipelineContext(Project project) {
            this.project = project;
            this.hasTests = project.hasTests();
        }

        public boolean mustRunDeployment() {
            return testsRanSuccessfully || !hasTests;
        }
    }
}