package com.az.gitember.controller;

import com.az.gitember.App;
import com.az.gitember.controller.handlers.CherryEventHandler;
import com.az.gitember.controller.handlers.CherryEventHandler.CherryData;
import com.az.gitember.data.*;
import com.az.gitember.service.Context;
import com.az.gitember.service.GitemberUtil;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jgit.api.CherryPickResult;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.javafx.StackedFontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Controlled for branch difference.
 */
public class CherryController implements Initializable {

    private final static Logger LOG = Logger.getLogger(CherryController.class.getName());

    private final ObservableList<PlotCommit<?>> commits = FXCollections.observableList(new ArrayList<>());
    private final Map<ObjectId, ObjectId> cherryInfo = new HashMap<>(); 
    
    private static final int HEIGH = 40;
    private int plotWidth = 20 * HEIGH;
    
    private HistoryPlotCommitRenderer plotCommitRenderer = new HistoryPlotCommitRenderer();

    @FXML
    public AnchorPane hostCherryViewPanel; 
    
    @FXML
    private TableColumn<PlotCommit<?>, Canvas> laneTableColumn;

    @FXML
    private TableColumn<PlotCommit<?>, String> dateTableColumn;

    @FXML
    private TableColumn<PlotCommit<?>, String> messageTableColumn;

    @FXML
    private TableColumn<PlotCommit<?>, String> authorTableColumn;

    @FXML
    public TableColumn<PlotCommit<?>, String> shaTableColumn;

    @FXML
    public TableColumn<PlotCommit<?>, StackedFontIcon> mergedTableColumn;
    
    @FXML
    public TableColumn<PlotCommit<?>, String> refTableColumn;

    @FXML
    private TableView<PlotCommit<?>> commitsTableView;
    
    @FXML
    private ContextMenu cherryMenu;

    public SplitPane splitPanel;
    public BorderPane mainBorderPanel;
    public Pane spacerPane;
    
    public TextField searchText;
    public TextField commitCount;
    private String upstreamBranchName;
    private String headBranchName;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> searchText.requestFocus());
        
        commitCount.setText("100");

        commitsTableView.setFixedCellSize(HEIGH);
        commitsTableView
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(new ChangeListener<PlotCommit<?>>() {

                    @Override
                    public void changed(final ObservableValue<? extends PlotCommit<?>> observable,
                                        final PlotCommit<?> oldValue,
                                        final PlotCommit<?> newValue) {

                        if (splitPanel.getItems().size() == 1) {
                        	hostCherryViewPanel = new AnchorPane();
                        	hostCherryViewPanel.prefHeight(330);
                        	hostCherryViewPanel.minHeight(250);
                            splitPanel.getItems().add(hostCherryViewPanel);
                            splitPanel.setDividerPositions(0.65);
                            mainBorderPanel.layout();
                        }
                        if (newValue != null) {
                            try {
                                Context.scmRevCommitDetails.setValue(Context.getGitRepoService().adapt(newValue));
                                final Parent commitView = App.loadFXML(Const.View.HISTORY_DETAIL).getFirst();
                                hostCherryViewPanel.getChildren().clear();
                                hostCherryViewPanel.getChildren().add(commitView);
                                
        						boolean hasCommit = cherryInfo.get(newValue.getId()) != null;
        						cherryMenu.getItems().forEach(menu -> menu.setDisable(hasCommit));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }

                });



        commitsTableView.setRowFactory(
                tr -> {
                    return new TableRow<PlotCommit<?>>() {
                        private String calculateStyle(final PlotCommit<?> plotCommit) {
                            String searchString = searchText.getText();
                            if (isSearchEligible(plotCommit, searchString)) {
                                Map<String, Set<String>> map = Context.searchResult.getValue();
                                if (map != null && map.containsKey(plotCommit.getName()) ) {
                                    return LookAndFeelSet.FOUND_ROW;
                                }
                            }
                            return "";
                        }

                        @Override
                        protected void updateItem(PlotCommit<?> item, boolean empty) {
                            super.updateItem(item, empty);
                            setStyle(calculateStyle(item));
                        }

                        private boolean isSearchEligible(PlotCommit<?> plotCommit, String searchString) {
                            return plotCommit != null
                                    && plotCommit.getName() != null
                                    && searchString != null
                                    && searchString.length() > Const.SEARCH_LIMIT_CHAR;
                        }
                    };
                }
        );

        laneTableColumn.setCellValueFactory(
                c -> {
                    return new ObservableValue<Canvas>() {
                        @Override
                        public Canvas getValue() {
                            Canvas canvas = new Canvas(plotWidth, HEIGH);
                            GraphicsContext gc = canvas.getGraphicsContext2D();
                            plotCommitRenderer.render(gc, c.getValue(), HEIGH);
                            return canvas;
                        }

                        @Override
                        public void addListener(InvalidationListener listener) {
                        }

                        @Override
                        public void removeListener(InvalidationListener listener) {
                        }

                        @Override
                        public void addListener(ChangeListener<? super Canvas> listener) {
                        }

                        @Override
                        public void removeListener(ChangeListener<? super Canvas> listener) {
                        }

                    };
                }
        );

        mergedTableColumn.setCellValueFactory(
        		c -> new ObservableValue<StackedFontIcon>() {
					@Override
					public StackedFontIcon getValue() {
						boolean hasCommit = cherryInfo.get(c.getValue().getId()) != null;

						if(hasCommit) {
							return GitemberUtil.create(new FontIcon(FontAwesome.PLUS_SQUARE_O));
						} else {
							return GitemberUtil.create(new FontIcon(FontAwesome.MINUS_SQUARE_O));
						}
					}

                    @Override
                    public void addListener(InvalidationListener listener) {
                    }

                    @Override
                    public void removeListener(InvalidationListener listener) {
                    }

					@Override
					public void addListener(ChangeListener<? super StackedFontIcon> listener) {
					}

					@Override
					public void removeListener(ChangeListener<? super StackedFontIcon> listener) {
					}
    			
				}
        );

        shaTableColumn.setCellValueFactory(
                c -> new ReadOnlyStringWrapper(c.getValue().getName())
        );

        refTableColumn.setCellValueFactory(
                c -> {
                    PlotCommit<?> plotCommit = c.getValue();
                    LinkedList<String> refs = new LinkedList<>();
                    for (int i = 0; i < plotCommit.getRefCount(); i++) {
                        refs.add(
                                plotCommit.getRef(i).getName()
                        );
                    }
                    return new ReadOnlyStringWrapper(refs.stream().collect(Collectors.joining(", ")));
                }
        );

        authorTableColumn.setCellValueFactory(
                c -> new ReadOnlyStringWrapper(c.getValue().getAuthorIdent().getName())
        );

        messageTableColumn.setCellValueFactory(
                c ->  {
                    return new ReadOnlyStringWrapper( c.getValue().getShortMessage().replace("\n","") );
                }
        );


        dateTableColumn.setCellValueFactory(
                c -> new ReadOnlyStringWrapper(
                        GitemberUtil.formatDate(GitemberUtil.intToDate(c.getValue().getCommitTime()))
                )
        );
        
        
        commitsTableView.setItems(commits);
    }
    
    public void setData(String headBranchName, String upstreamBranchName, CherryData data) {
    	this.headBranchName = headBranchName;
    	this.upstreamBranchName = upstreamBranchName;
    	this.commits.addAll(data.getPlotData());
    	this.cherryInfo.clear();
    	this.cherryInfo.putAll(data.getCherryInfo());
    }
    
    public void refreshCommitsClickHandler(ActionEvent actionEvent) {
    	new CherryEventHandler(headBranchName, upstreamBranchName, Integer.parseInt(commitCount.getText())).handle(actionEvent);
    	((javafx.stage.Stage)((Button)actionEvent.getSource()).getScene().getWindow()).close();
    }
    
    
    public void cherryPickMenuItemClickHandler(ActionEvent actionEvent) {

        final RevCommit revCommit = (RevCommit) commitsTableView.getSelectionModel().getSelectedItem();

        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Please confirm");
        alert.setHeaderText("Cherry pick");
        alert.setContentText("Do you really want to apply changes from \n"
                + revCommit.getName() + " ?");
        alert.initOwner(App.getScene().getWindow());
        alert.setWidth(alert.getWidth() * 2);
        alert.setHeight(alert.getHeight() * 1.5);

        alert.showAndWait().ifPresent( r-> {

            try {
                if (r == ButtonType.OK) {
                    CherryPickResult cherryPickResult = Context.getGitRepoService().cherryPick(revCommit);
                }
            } catch (IOException e) {
                Context.getMain().showResult("Cherry pick is failed ",
                        ExceptionUtils.getRootCause(e).getMessage(), Alert.AlertType.ERROR);
            } finally {
                Context.updateStatus(null);
            }

        } );

    }
}
