import dependencies.Config;
import dependencies.Emailer;
import dependencies.Logger;
import dependencies.Project;

public class Pipeline {
    private final Config config;
    private final Emailer emailer;
    private final Logger log;
    private static final String SUCCESS = "success";

    public Pipeline(Config config, Emailer emailer, Logger log) {
        this.config = config;
        this.emailer = emailer;
        this.log = log;
    }

    public void run(Project project) {
        if (haveTestsFailed(project)) {
            sendEmail("Tests failed");
            return;
        }

        if (hasDeploymentFailed(project)) {
            sendEmail("Deployment failed");
            return;
        }
        sendEmail("Deployment completed successfully");
    }

    private boolean haveTestsFailed(Project project) {
        if (!project.hasTests()) {
            log.info("No tests");
            return false;
        }

        if (project.runTests().equals(SUCCESS)) {
            log.info("Tests passed");
            return false;
        }

        log.error("Tests failed");
        return true;
    }

    private boolean hasDeploymentFailed(Project project) {
        if (!project.deploy().equals(SUCCESS)) {
            log.error("Deployment failed");
            return true;
        }
        log.info("Deployment successful");
        return false;
    }

    private void sendEmail(String text) {
        if (config.sendEmailSummary()) {
            log.info("Sending email");
            emailer.send(text);
        } else {
            log.info("Email disabled");
        }
    }
}