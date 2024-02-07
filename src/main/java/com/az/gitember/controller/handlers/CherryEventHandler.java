package com.az.gitember.controller.handlers;

import com.az.gitember.App;
import com.az.gitember.controller.CherryController;
import com.az.gitember.controller.BranchDiffController;
import com.az.gitember.controller.DefaultProgressMonitor;
import com.az.gitember.controller.Main;
import com.az.gitember.data.Const;
import com.az.gitember.service.Context;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class CherryEventHandler extends AbstractLongTaskEventHandler implements EventHandler<ActionEvent> {
	
	private final static Logger LOG = Logger.getLogger(CherryEventHandler.class.getName());

    private final String upstreamBranchName;
    private final String headBranchName;
    private final Integer limit;

    public CherryEventHandler(String headBranchName, String upstreamBranchName, Integer limit) {
        this.upstreamBranchName = upstreamBranchName;
        this.headBranchName = headBranchName;
        this.limit = limit;
    }

    @Override
    public void handle(ActionEvent event) {
        Task<PlotCommitList<PlotLane>> longTask = new Task<PlotCommitList<PlotLane>>() {
            @Override
            protected PlotCommitList<PlotLane> call() throws Exception {
            	try {
//	            	Context.getGitRepoService().cherry(headBranchName, upstreamBranchName, null, new DefaultProgressMonitor((t, d) -> {
//	                    updateTitle(t);
//	                    updateProgress(d, 1.0);
//	                }));
                	System.out.println(Context.getGitRepoService().branchDiff(upstreamBranchName, headBranchName, new DefaultProgressMonitor((t, d) -> {
	                    updateTitle(t);
	                    updateProgress(d, 1.0);
	                })));
            	}catch(Exception e) {
            		LOG.severe(e.getMessage());
            	}
            	
            	
                return Context.getGitRepoService().getCommitsByTree(headBranchName, false, limit, new DefaultProgressMonitor((t, d) -> {
                    updateTitle(t);
                    updateProgress(d, 1.0);
                }));
            }
        };

        launchLongTask(
                longTask,
                o -> {
                	PlotCommitList<PlotLane> commits = (PlotCommitList<PlotLane>) o.getSource().getValue();
                    try {
                        CherryController cherryController =
                                (CherryController) App.loadFXMLToNewStage(Const.View.CHERRY, "Cherry " + headBranchName + " -> " + upstreamBranchName).getSecond();
                        cherryController.setData(headBranchName, upstreamBranchName, commits);
                    } catch (IOException e) {
                    	LOG.severe(e.getMessage());
                    }
                },
                o -> Main.showResult("Cherry", "Failed to cherry between " + upstreamBranchName + " and " + headBranchName, Alert.AlertType.ERROR)
        );
    }


}
