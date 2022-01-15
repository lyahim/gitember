package com.az.gitember.gitlab;

import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import org.gitlab4j.api.models.Issue;

import java.net.URL;
import java.util.ResourceBundle;

public class IssueDetailController implements Initializable {

    public Label numLabel;
    public Label titleLabel;
    private Issue issue;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println(issue);
    }

    public void setIssue(Issue issue) {
        this.issue = issue;
    }

}