package com.az.gitember;

import com.az.gitember.misc.*;
import com.az.gitember.scm.exception.GEScmAPIException;
import com.az.gitember.ui.CommitDialog;
import com.az.gitember.ui.StatusCellValueFactory;
import com.sun.javafx.binding.StringConstant;
import difflib.DiffUtils;
import difflib.Patch;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Igor_Azarny on 23.12.2016.
 */
public class WorkingCopyController implements Initializable {

    private final static Logger log = Logger.getLogger(FXMLController.class.getName());

    public TableView workingCopyTableView;
    public TableColumn<ScmItem, FontIcon> statusTableColumn;
    public TableColumn<ScmItem, Boolean> selectTableColumn;
    public TableColumn<ScmItem, String> itemTableColumn;
    public Button stashBtn;
    public Button commitBtn;
    private Button stageAllBtn;
    private Button refreshBtn;

    public Menu workingCopyMenu;

    private ScmBranch branch;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        itemTableColumn.setCellValueFactory(
                c -> StringConstant.valueOf(c.getValue().getShortName())
        );

        statusTableColumn.setCellValueFactory(
                c -> new StatusCellValueFactory(c.getValue().getAttribute().getStatus())
        ); //TODO add tooltip

        selectTableColumn.setCellValueFactory(
                c -> new ReadOnlyBooleanWrapper(!isUnstaged(c.getValue()))
        );

        selectTableColumn.setCellFactory(p -> new CheckBoxTableCell<ScmItem, Boolean>());

        workingCopyTableView.setRowFactory(
                tr -> {
                    return new TableRow<ScmItem>() {
                        @Override
                        protected void updateItem(ScmItem item, boolean empty) {
                            super.updateItem(item, empty);
                            super.updateItem(item, empty);
                            if (item == null) {
                                setStyle("");
                            } else if (item.getAttribute().getStatus().contains(ScmItemStatus.MODIFIED)) {
                                //todo styles
                            } else if (item.getAttribute().getStatus().contains(ScmItemStatus.MISSED)) {
                            } else if (item.getAttribute().getStatus().contains(ScmItemStatus.ADDED)) {
                            } else if (item.getAttribute().getStatus().contains(ScmItemStatus.REMOVED)) {
                            } else if (item.getAttribute().getStatus().contains(ScmItemStatus.UNTRACKED)) {

                            }
                        }
                    };
                }
        );

        stageAllBtn = new Button("Stage all");
        stageAllBtn.setOnAction(this::stageAllBtnHandler);
        stageAllBtn.setId(Const.MERGED);

        commitBtn = new Button("Commit ...");
        commitBtn.setOnAction(this::commitBtnHandler);
        commitBtn.setId(Const.MERGED);

        stashBtn = new Button("Move to stash");
        stashBtn.setOnAction(this::stashBtnHandler);
        stashBtn.setId(Const.MERGED);

        refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(this::refreshBtnHandler);
        refreshBtn.setId(Const.MERGED);
        refreshBtn.setGraphic(
                new FontIcon(FontAwesome.REFRESH)
        );

    }

    public void open(final ScmBranch branch, final String path) {

        this.branch = branch;

        GitemberApp.getMainStage().getScene().setCursor(Cursor.WAIT);

        Task<List<ScmItem>> longTask = new Task<List<ScmItem>>() {
            @Override
            protected List<ScmItem> call() throws Exception {
                return GitemberApp.getRepositoryService().getStatuses(path);
            }
        };

        longTask.setOnSucceeded(z -> Platform.runLater(
                () -> {
                    List<ScmItem> list = longTask.getValue();
                    if (path == null) {
                        workingCopyTableView.setItems(FXCollections.observableArrayList(list));
                    } else {
                        if (list.size() == 1) {
                            //in case of delete on changed file. but not such operation atm.
                            ScmItem item = list.get(0);
                            workingCopyTableView.getItems().replaceAll(o -> {
                                if (((ScmItem) o).getShortName().endsWith(path)) {
                                    return item;
                                }
                                return o;
                            });
                        } else {
                            //revert operation
                            workingCopyTableView.getItems().removeIf(o -> ((ScmItem) o).getShortName().equals(path));
                        }
                    }
                    GitemberApp.getMainStage().getScene().setCursor(Cursor.DEFAULT);
                }
                )
        );

        longTask.setOnFailed(z -> Platform.runLater(
                () -> {
                    GitemberApp.getMainStage().getScene().setCursor(Cursor.DEFAULT);
                    Throwable e = z.getSource().getException();
                    GitemberApp.showResult("Cannot open working copy. " + e == null ? "" : e.getMessage(),
                            Alert.AlertType.ERROR);
                    log.log(Level.SEVERE, "Cannot load item statuses from repository", e);
                }
                )
        );

        Platform.runLater(
                () -> {
                    Thread th = new Thread(longTask);
                    th.start();

                }
        );

    }

    //---------------------------------------------------------------------------------------------------------------//
    //---------------------------------------------------------------------------------------------------------------//
    //-----------------------------    ToolBar handlers       -------------------------------------------------------//
    //---------------------------------------------------------------------------------------------------------------//
    //---------------------------------------------------------------------------------------------------------------//


    /**
     * Stage all changes for commit.
     *
     * @param actionEvent event
     */
    @SuppressWarnings({"unchecked", "unused"})
    public void stageAllBtnHandler(ActionEvent actionEvent) {
        workingCopyTableView.getItems().stream().forEach(i -> stageItem((ScmItem) i));
        workingCopyTableView.refresh();
    }


    /**
     * Commit all staged changes.
     *
     * @param actionEvent event
     * @throws Exception in case of errors
     */
    @SuppressWarnings("unused")
    public void commitBtnHandler(ActionEvent actionEvent) {
        CommitDialog dialog = new CommitDialog(
                "TODO history of commit messasge",
                GitemberApp.getRepositoryService().getUserName(),
                GitemberApp.getRepositoryService().getUserEmail()
        );
        dialog.setTitle("Commit message");
        dialog.setHeaderText("Provide commit message");
        dialog.setContentText("Message:");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                GitemberApp.getRepositoryService().setUserEmail(dialog.getUserEmail());
                GitemberApp.getRepositoryService().setUserName(dialog.getUserName());
                GitemberApp.getRepositoryService().commit(result.get());
                open(branch, null);
            } catch (GEScmAPIException e) {
                GitemberApp.showResult("Cannot commit, because: " + e.getMessage(), Alert.AlertType.ERROR);
                log.log(Level.SEVERE, "Cannot commit", e);

            }
        }
    }

    /**
     * Refresh changes from disk
     *
     * @param actionEvent event
     * @throws Exception
     */
    @SuppressWarnings("unused")
    public void refreshBtnHandler(ActionEvent actionEvent) {
        open(branch, null);
    }

    /**
     * Move changes to stash.
     *
     * @param actionEvent event
     * @throws Exception
     */
    @SuppressWarnings("unused")
    public void stashBtnHandler(ActionEvent actionEvent) {
        try {
            GitemberApp.getRepositoryService().stash();
            open(branch, null);
        } catch (GEScmAPIException e) {
            GitemberApp.showResult("Changes not moved to stash, because: " + e.getMessage(), Alert.AlertType.ERROR);
            log.log(Level.SEVERE, "Cannot move to stash", e);

        }

    }


    //---------------------------------------------------------------------------------------------------------------//
    //---------------------------------------------------------------------------------------------------------------//
    //-----------------------------    Item context menu item -------------------------------------------------------//
    //---------------------------------------------------------------------------------------------------------------//
    //---------------------------------------------------------------------------------------------------------------//


    /**
     * Add file to stage from context menu.
     *
     * @param event event
     */
    @SuppressWarnings("unused")
    public void addItemToStageMiEventHandler(Event event) {
        ScmItem item = (ScmItem) workingCopyTableView.getSelectionModel().getSelectedItem();
        if (item != null) {
            stageItem(item);
            workingCopyTableView.refresh();
        }
    }

    /**
     * Open file.
     *
     * @param actionEvent event
     */
    @SuppressWarnings("unused")
    public void openEventHandler(ActionEvent actionEvent) {
        final ScmItem item = (ScmItem) workingCopyTableView.getSelectionModel().getSelectedItem();
        if (item != null) {
            final FileViewController fileViewController = new FileViewController();
            try {
                fileViewController.openFile(
                        GitemberApp.getCurrentRepositoryPathWOGit() + File.separator + item.getShortName(),
                        item.getShortName());
            } catch (Exception e) {
                String msg = String.format("Cannot open file %s", item.getShortName());
                GitemberApp.showResult(msg, Alert.AlertType.WARNING);
                log.log(Level.WARNING, msg, e);
            }

        }
    }

    /**
     * Revert file changes.
     *
     * @param actionEvent event
     */
    @SuppressWarnings("unused")
    public void revertEventHandler(ActionEvent actionEvent) {
        final ScmItem item = (ScmItem) workingCopyTableView.getSelectionModel().getSelectedItem();
        if (item != null) {
            Optional<ButtonType> result = GitemberApp.showResult("Revert " + item.getShortName() + " changes ?", Alert.AlertType.CONFIRMATION);
            if (result.isPresent() && result.get() == ButtonType.OK) {
                GitemberApp.getRepositoryService().checkoutFile(item.getShortName());
                open(branch, item.getShortName());
            }
        }
    }

    /**
     * Show different with last version from repository.
     *
     * @param actionEvent event
     */
    @SuppressWarnings("unused")
    public void diffEventHandler(ActionEvent actionEvent) {
        final ScmItem item = (ScmItem) workingCopyTableView.getSelectionModel().getSelectedItem();
        if (item != null) {

            try {
                final String fileName = item.getShortName();
                final Pair<String, String> head = GitemberApp.getRepositoryService().getHead();
                final String oldFile = GitemberApp.getRepositoryService().saveFile(
                        head.getFirst(),
                        head.getSecond(), fileName);
                final String newFile = GitemberApp.getCurrentRepositoryPathWOGit() + File.separator + fileName;

                List<String> newFileLines = Files.readAllLines(Paths.get(newFile));
                List<String> oldFileLines = Files.readAllLines(Paths.get(oldFile));
                Patch<String> pathc = DiffUtils.diff(oldFileLines, newFileLines);

                final DiffViewController fileViewController = new DiffViewController();
                fileViewController.openFile(
                        new File(fileName).getName(),
                        oldFile, head.getSecond(),
                        newFile, "On disk",
                        pathc);

            } catch (Exception e) {
                log.log(Level.SEVERE, "Cannot get head", e);
            }
        }
    }

    /**
     * Open hisotry
     *
     * @param actionEvent event
     */
    @SuppressWarnings("unused")
    public void historyEventHandler(ActionEvent actionEvent) {
        final ScmItem item = (ScmItem) workingCopyTableView.getSelectionModel().getSelectedItem();
        if (item != null) {
            try {
                HistoryViewController.openHistoryWindow(item.getShortName(), branch.getFullName());
            } catch (Exception e) {
                log.log(Level.SEVERE, String.format("Cannot open history for %s %s", item.getShortName(), branch.getFullName()));
            }
        }
    }


    private boolean isUnstaged(ScmItem scmItem) {
        return scmItem.getAttribute().getStatus().contains(ScmItemStatus.MODIFIED)
                || scmItem.getAttribute().getStatus().contains(ScmItemStatus.MISSED)
                || scmItem.getAttribute().getStatus().contains(ScmItemStatus.UNTRACKED);
    }


    /**
     * Add file to stage.
     *
     * @param event event
     */
    @SuppressWarnings("unused")
    public void addItemToStageEventHandler(Event event) {
        if (event.getTarget() instanceof CheckBoxTableCell) {
            CheckBoxTableCell cell = (CheckBoxTableCell) event.getTarget();
            if (cell.getTableColumn() == this.selectTableColumn) {
                ScmItem item = (ScmItem) workingCopyTableView.getSelectionModel().getSelectedItem();
                stageItem(item);
                workingCopyTableView.refresh();
            }
            //TODO V2 unstage changes
        }
    }


    private void stageItem(ScmItem item) {
        try {
            if (item != null && isUnstaged(item)) {
                if (item.getAttribute().getStatus().contains(ScmItemStatus.MISSED)) {
                    GitemberApp.getRepositoryService().removeMissedFile(item.getShortName());
                    item.getAttribute().getStatus().remove(ScmItemStatus.MISSED);
                    item.getAttribute().getStatus().add(ScmItemStatus.REMOVED);
                } else if (item.getAttribute().getStatus().contains(ScmItemStatus.UNTRACKED)) {
                    GitemberApp.getRepositoryService().addFileToCommitStage(item.getShortName());
                    item.getAttribute().getStatus().remove(ScmItemStatus.UNTRACKED);
                    item.getAttribute().getStatus().add(ScmItemStatus.ADDED);
                    item.getAttribute().getStatus().add(ScmItemStatus.CHANGED);
                    item.getAttribute().getStatus().add(ScmItemStatus.UNCOMMITED);
                } else {
                    GitemberApp.getRepositoryService().addFileToCommitStage(item.getShortName());
                    item.getAttribute().getStatus().remove(ScmItemStatus.MODIFIED);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    public static Parent openWorkingCopyHandler(ScmBranch branch, MenuBar menuBar, ToolBar toolBar) {

        final FXMLLoader fxmlLoader = new FXMLLoader();
        try (InputStream is = WorkingCopyController.class.getResource("/fxml/WorkingCopyPane.fxml").openStream()) {
            final Parent workCopyView = fxmlLoader.load(is);
            final WorkingCopyController workingCopyController = fxmlLoader.getController();
            workingCopyController.open(branch, null);

            menuBar.getMenus().add(2, workingCopyController.workingCopyMenu);

            toolBar.getItems().add(8, workingCopyController.refreshBtn);
            toolBar.getItems().add(8, workingCopyController.stashBtn);
            toolBar.getItems().add(8, workingCopyController.commitBtn);
            toolBar.getItems().add(8, workingCopyController.stageAllBtn);

            return workCopyView;
        } catch (IOException ioe) {
            log.log(Level.SEVERE, "Cannot open working copy view", ioe.getMessage());
        }

        return null;


    }


}
