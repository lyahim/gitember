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
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class CherryEventHandler extends AbstractLongTaskEventHandler implements EventHandler<ActionEvent> {
	
	public static class CherryData {
		private PlotCommitList<PlotLane> plotData;
		private Map<ObjectId, ObjectId> cherryInfo;
		
		public CherryData(PlotCommitList<PlotLane> plotData, Map<ObjectId, ObjectId> cherryInfo) {
			this.plotData = plotData;
			this.cherryInfo = cherryInfo;
		}
		
		public PlotCommitList<PlotLane> getPlotData() {
			return plotData;
		}
		public Map<ObjectId, ObjectId> getCherryInfo() {
			return cherryInfo;
		}
	}
	
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
        Task<CherryData> longTask = new Task<CherryData>() {
            @Override
            protected CherryData call() throws Exception {
            	try {
                	return new CherryData(Context.getGitRepoService().getCommitsByTree(headBranchName, false, limit, new DefaultProgressMonitor((t, d) -> {
                        updateTitle(t);
                        updateProgress(d, 1.0);
                    })), //
        			Context.getGitRepoService().cherry(headBranchName, upstreamBranchName, limit, new DefaultProgressMonitor((t, d) -> {
                        updateTitle(t);
                        updateProgress(d, 1.0);
                    })));
            	}catch(Exception e) {
            		LOG.severe(e.getMessage());
            	}
            	return null;
            }
        };

        launchLongTask(
                longTask,
                o -> {
                	CherryData data = (CherryData) o.getSource().getValue();
                    try {
                        CherryController cherryController =
                                (CherryController) App.loadFXMLToNewStage(Const.View.CHERRY, "Cherry " + headBranchName + " -> " + upstreamBranchName).getSecond();
                        cherryController.setData(headBranchName, upstreamBranchName, data);
                    } catch (IOException e) {
                    	LOG.severe(e.getMessage());
                    }
                },
                o -> Main.showResult("Cherry", "Failed to cherry between " + upstreamBranchName + " and " + headBranchName, Alert.AlertType.ERROR)
        );
    }


}
