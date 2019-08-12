package io.jenkins.plugins;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class WebsocketInNotifier extends Notifier {

    private final String url;
    private final String additionalHttpHeaders;
    private final String startMessage;

    @DataBoundConstructor
    public WebsocketInNotifier(String url, String startMessage, String additionalHttpHeaders){
        this.url = url;
        this.startMessage = startMessage;
        this.additionalHttpHeaders = additionalHttpHeaders;
    }

    public String getUrl() {
        return url;
    }

    public String getAdditionalHttpHeaders() {
        return additionalHttpHeaders;
    }

    public String getStartMessage() {
        return startMessage;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        sendMessageBlocking(build, listener);
        return true;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        AbstractBuild<?,?> previousBuild = build.getPreviousBuild();
        if(previousBuild != null) {
            sendMessageBlocking(previousBuild, listener);
        } else {
            sendMessageBlocking(build, listener);
        }
        return true;
    }

    private void sendMessageBlocking(AbstractBuild<?, ?> build, BuildListener listener) {
        JSONObject json = getBuildJson(build);
        listener.getLogger().println("Websocket connection to: " + this.url);

        try {
            EnvVars environment = build.getEnvironment(listener);
            String expandedUrl = environment.expand(url);
            String expandedStartMessage = environment.expand(startMessage);
            String expandedAdditionalHttpHeaders= environment.expand(additionalHttpHeaders);

            WebsocketInClient websocketClient = new WebsocketInClient(new URI(expandedUrl), expandedStartMessage, DescriptorImpl.toMap(expandedAdditionalHttpHeaders));
            listener.getLogger().println("Connected: "+websocketClient.connectBlocking());
            websocketClient.send(json.toString());
            websocketClient.closeBlocking();
        } catch (URISyntaxException | InterruptedException | IOException e) {
            listener.getLogger().println(e.getMessage());
        }
    }

    private JSONObject getBuildJson(AbstractBuild<?, ?> build) {
        String resultStatus = null;
        Result result = build.getResult();
        if(!build.isBuilding() && build.getPreviousBuild() != null) {
            result = build.getPreviousBuild().getResult();
        }
        if(result != null) {
            resultStatus = result.toString();
        }

        String fullUrl = null;
        if(Jenkins.get().getRootUrl() != null) {
            fullUrl = Jenkins.get().getRootUrl() + build.getUrl();
        }
        return new JSONObject()
                .element("name", build.getProject().getName())
                .element("url", build.getProject().getUrl())
                .element("build", new JSONObject()
                        .element("full_url", fullUrl)
                        .element("number", build.getNumber())
                        .element("phase", build.isBuilding() ? "COMPLETED" : "STARTED")
                        .element("status", resultStatus)
                        .element("url", build.getUrl())
                );
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl  extends BuildStepDescriptor<Publisher> {

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        public String getDisplayName() {
            return "Websocket.in Notifier";
        }

        public FormValidation doCheckUrl(@QueryParameter String url) {
            try{
                if(!url.startsWith("ws")) {
                    return FormValidation.error(url+" needs to start with ws:// or wss://");
                }
                new URI(url);
                return FormValidation.ok();
            } catch(Exception ex){
                return FormValidation.error("'" + url + "' is not a valid URL.");
            }
        }

        public FormValidation doCheckAdditionalHttpHeaders(@QueryParameter String additionalHttpHeaders) {
            try{
                if(additionalHttpHeaders != null && !additionalHttpHeaders.isEmpty()) {
                    Map<String,String> headers = toMap(additionalHttpHeaders);
                    if(headers.isEmpty()) {
                        return FormValidation.warning("No headers detected");
                    }
                    String headerWord = headers.size() == 1 ? "header" : "headers";
                    return FormValidation.ok(headers.size() + " additional http "+headerWord+" found.");
                }
                return FormValidation.ok();
            } catch(Exception ex){
                return FormValidation.error("Not a valid property format");
            }
        }

        public FormValidation doTestConnection(@QueryParameter("url") final String url,
                                               @QueryParameter("startMessage") final String startMessage,
                                               @QueryParameter("additionalHttpHeaders") final String additionalHttpHeaders
                                               ) {
            try {
                Map<String,String> headers = null;
                if(additionalHttpHeaders != null && !additionalHttpHeaders.equals("")) {
                    headers = toMap(additionalHttpHeaders);
                }
                WebsocketInClient client = new WebsocketInClient(new URI(url), startMessage, headers);
                client.connectBlocking();
                boolean isSuccessful = false;
                if(client.isOpen()) {
                    isSuccessful = true;
                }
                client.closeBlocking();
                if(isSuccessful) {
                    return FormValidation.ok("Connection was successful.");
                }
                return FormValidation.error("Error connecting: " + client.getException().getLocalizedMessage());
            } catch (Exception e) {
                return FormValidation.error("Client error :  "+ e.getMessage());
            }
        }

        public static Map<String, String> toMap(String value) throws IOException {
            if(value == null) {
                return null;
            }
            Properties properties = new Properties();
            properties.load(new StringReader(value));
            return properties.entrySet().stream().collect(Collectors.toMap(
                    e -> String.valueOf(e.getKey()),
                    e -> String.valueOf(e.getValue())));
        }
    }
}
