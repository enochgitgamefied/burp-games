package dev.velocity;

/** Fixed educational scenarios, independent of Burp traffic and external applications. */
final class Missions {
    record Question(String title,String evidence,String prompt,String[] answers,int correct,String explanation,String hint) {}
    static final String[] NAMES={"Source City","Pipeline Pass","Runtime Rush","Release Ridge"};
    static final String[] THEMES={"SAST / SOURCE REVIEW","CI/CD / FINDING TRIAGE","DAST / RUNTIME EVIDENCE","SAST + DAST / RELEASE GATE"};
    static final Question[][] QUESTIONS={
        {
            new Question("Static or dynamic?","A check reviews source files before the app starts.","Which testing approach is this?",new String[]{"SAST: analyze code without executing the app","DAST: observe the running application","Neither: security checks need production traffic"},0,"SAST analyzes code or related program artifacts without running the application.","Think about whether the application is running."),
            new Question("Protect the login secret","Review note: the password is included in a debug log.","Which fix addresses the risk?",new String[]{"Move the same log statement to another function","Remove the secret from logs and retain safe event metadata","Hide the log viewer's navigation link"},1,"Keep passwords and other secrets out of logs. Record useful event metadata without the sensitive value.","Fix the sensitive data flow, not the visibility of the viewer.")
        },
        {
            new Question("Triage before closing","A static finding points to a bundled test fixture.","What should the reviewer do next?",new String[]{"Suppress every finding from the same rule","Assume it is exploitable in production","Check reachability and usage; document any scoped suppression"},2,"Review the evidence and deployment context. A test-file location alone is not enough to declare a false positive.","A finding needs context and a recorded rationale."),
            new Question("Build gate","A confirmed high-impact defect has a reviewed source fix.","What evidence belongs at the gate?",new String[]{"A renamed issue ticket","Relevant regression checks and a rerun of the affected analysis","A screenshot showing zero warnings after disabling rules"},1,"Verify the fix and rerun relevant checks. Disabling detection does not demonstrate remediation.","The gate should show that the defect is fixed.")
        },
        {
            new Question("Runtime observation","A test examines responses from a running staging application.","Which approach matches this evidence?",new String[]{"DAST: test application behavior at runtime","SAST: only inspect unexecuted source files","A successful test proves the entire app is secure"},0,"DAST examines a running application. Its coverage depends on the paths, roles and behavior exercised.","The application is running during the check."),
            new Question("Coverage matters","A runtime report covered only unauthenticated pages.","What is the most accurate conclusion?",new String[]{"All authenticated paths are also safe","Static analysis is now unnecessary","Authenticated behavior still needs appropriately scoped testing"},2,"A clean report applies to the tested coverage. Different roles and states may expose different behavior.","Ask which application paths the report actually covered.")
        },
        {
            new Question("Connect the evidence","Source review and a runtime test flag the same data-handling defect.","How should the team track it?",new String[]{"Count it as two unrelated vulnerabilities","Link both observations to the root cause and its fix","Delete the source finding because runtime evidence exists"},1,"Correlate related evidence while preserving its context. Both views can help confirm the root cause and remediation.","Keep evidence without duplicating the underlying work."),
            new Question("Release decision","The fix is merged. The original checks have not been rerun.","What completes the remediation cycle?",new String[]{"Retest the affected source and runtime behavior, then review remaining risk","Ship because a merge proves the fix works","Remove the original finding from the report"},0,"Retest relevant source and runtime behavior. Release decisions also account for remaining risk and coverage.","A changed file is not yet evidence of a verified fix.")
        }
    };
}
