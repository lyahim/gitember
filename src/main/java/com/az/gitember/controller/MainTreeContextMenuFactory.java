package com.az.gitember.controller;

import com.az.gitember.controller.handlers.*;
import com.az.gitember.data.ScmBranch;
import com.az.gitember.data.ScmRevisionInformation;
import com.az.gitember.service.Context;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class MainTreeContextMenuFactory {

    ContextMenu createContextMenu(final ScmBranch branchItem) {

        boolean disablePool = branchItem.getRemoteMergeName() == null &&  ScmBranch.BranchType.LOCAL.equals(branchItem.getBranchType());
        final String name = branchItem.getShortName();
        String fullName = name;
        if (branchItem.getRemoteMergeName() != null) {
            fullName += " (" + branchItem.getRemoteMergeName() + ")";
        }

        ContextMenu cm = new ContextMenu();

        MenuItem checkoutMI = new MenuItem("Checkout");
        checkoutMI.setOnAction(new CheckoutBranchEventHandler(branchItem));

        MenuItem branchMI = new MenuItem("Create branch ...");
        branchMI.setOnAction( new CreateBranchEventHandler(branchItem) );


        MenuItem deleteMI = new MenuItem("Delete "+ branchItem.getShortName() + "...");
        deleteMI.setOnAction(new DeleteBranchEventHandler(branchItem));

        MenuItem pullMI = new MenuItem("Pull " + (disablePool ? "" : fullName));
        MenuItem pushMI = new MenuItem("Push " + (name == fullName ? (name + " ...") : (fullName)));

        cm.getItems().add(checkoutMI);
        cm.getItems().add(branchMI);

        if (!branchItem.getShortName().equals(Context.workingBranch.getValue().getShortName()) && ScmBranch.BranchType.TAG != branchItem.getBranchType()) {

            MenuItem mergeMI = new MenuItem("Merge "+ branchItem.getShortName() + " -> " + Context.workingBranch.getValue().getShortName() +  "...");
            mergeMI.setOnAction(new MergeBranchEventHandler(branchItem.getFullName()));

            MenuItem rebaseMI = new MenuItem("Rebase "+ branchItem.getShortName() + " -> " + Context.workingBranch.getValue().getShortName() +  "...");
            rebaseMI.setOnAction(new RebaseBranchEventHandler(branchItem.getFullName()));

            cm.getItems().add(mergeMI);
            cm.getItems().add(rebaseMI);



        }

        if (branchItem.getBranchType().equals(ScmBranch.BranchType.LOCAL)) {
            cm.getItems().add(new SeparatorMenuItem());
            cm.getItems().add(pullMI);
            cm.getItems().add(pushMI);
        }

        cm.getItems().add(new SeparatorMenuItem());
        cm.getItems().add(deleteMI);

        pullMI.setDisable(disablePool);
        if (!disablePool) {
            pullMI.setOnAction(new PullHandler(branchItem));
        }
        pushMI.setOnAction( new PushHandler(branchItem) );

        if (ScmBranch.BranchType.TAG == branchItem.getBranchType()) {
            cm.getItems().removeAll(pullMI, pushMI);
        }

        //Add diff submenu
        int totalBranches = Context.localBrancesProperty.get().size() +
                Context.remoteBrancesProperty.get().size();
        if (totalBranches > 1) { // need at least 2 branches to create diff
			Menu branchDiffMI = new Menu("Diff with");
			List<ScmBranch> branches = new ArrayList<>(Context.localBrancesProperty.get());
			branches.addAll(Context.remoteBrancesProperty.get());
			fillBranchList(branchDiffMI, branches, branchItem.getFullName(),
					(branchName, rightBranchName) -> new BranchDiffEventHandler(branchName, rightBranchName));
			cm.getItems().add(new SeparatorMenuItem());
			cm.getItems().add(branchDiffMI);
        }
        
        if(Context.localBrancesProperty.get().size() > 1 && Context.localBrancesProperty.get().contains(branchItem)) {
			Menu branchCherryMI = new Menu("Cherry with");
			fillBranchList(branchCherryMI, Context.localBrancesProperty.get(), branchItem.getFullName(),
					(headBranchName, upstreamBranchName) -> new CherryEventHandler(headBranchName, upstreamBranchName, 100));
			cm.getItems().add(branchCherryMI);
        }
        
        return cm;
    }


    private void fillBranchList(Menu menuItem, List<ScmBranch> scmBranches, String branchName, BiFunction<String, String, EventHandler<ActionEvent>> actionHandler) {
        scmBranches.stream()
                .filter( br -> !br.getFullName().equals(branchName))
                .forEach(br -> {
                    String rightBranchName = br.getFullName();
                    MenuItem mi = new MenuItem(rightBranchName);
                    mi.setOnAction(actionHandler.apply(branchName, rightBranchName));
                    menuItem.getItems().add(mi);
                    
                });
    }



    ContextMenu createContextMenu(ScmRevisionInformation scmItem) {

        MenuItem applyStashMI = new MenuItem("Apply stash ...");
        applyStashMI.setOnAction(new ApplyStashEventHandler(scmItem));

        MenuItem deleteStashMI = new MenuItem("Delete stash ...");
        deleteStashMI.setOnAction(new DeleteStashEventHandler(scmItem));

        return new ContextMenu(
                applyStashMI,
                new SeparatorMenuItem(),
                deleteStashMI
        );
    }


    ContextMenu createSearchContextMenu() {
        MenuItem searchMenuItem = new MenuItem("Search ...");
        searchMenuItem.setOnAction(new MainTreeBranchSearchHandler(searchMenuItem));
        return new ContextMenu(searchMenuItem);
    }

    ContextMenu createTagContextMenu() {

        MenuItem createTagMenuItem = new MenuItem("Create tag ...");
        createTagMenuItem.setOnAction(new CreateTagEventHandler());

        MenuItem searchMenuItem = new MenuItem("Search ...");
        searchMenuItem.setOnAction(new MainTreeBranchSearchHandler(searchMenuItem));


        return new ContextMenu(searchMenuItem, new SeparatorMenuItem(), createTagMenuItem);
    }
}
