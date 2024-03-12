package com.awscloudprojects;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class CdkApp {
    public static void main(final String[] args) {
        App app = new App();
        new CdkStack(app, "CdkStack", StackProps.builder().build());
        app.synth();
    }
}

