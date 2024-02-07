/*
 * Copyright (C) 2024 Mihaly Szlauko <lyahim@gmail.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.api2;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * A class used to execute a {@code git-cherry} command. It has setters for all
 * supported options and arguments of this command and a {@link #call()} method
 * to finally execute the command.
 *
 * @see <a href=
 *      "http://www.kernel.org/pub/software/scm/git/docs/git-cherry.html" >Git
 *      documentation about git-cherry</a>
 */
public class CherryCommand extends GitCommand<Map<RevCommit, Boolean>> {

	private String head;
	private String upstream;
	private String limitSha;

	private ProgressMonitor monitor = NullProgressMonitor.INSTANCE;

	/**
	 * Constructor for CherryCommand
	 *
	 * @param repo the {@link org.eclipse.jgit.lib.Repository}
	 * @throws IOException
	 * @throws NoHeadException
	 */
	public CherryCommand(Repository repo) { // TODO set protected
		super(repo);
	}

	@Override
	public Map<RevCommit, Boolean> call() throws NoHeadException {
		checkCallable();
		try {
			if (StringUtils.isNoneEmpty(head, upstream)) {
				setCallable(false);
				try (RevWalk revWalk = new RevWalk(repo)) {
					revWalk.sort(RevSort.REVERSE);

					final ObjectId rootId = repo.resolve(head);
					final RevCommit root = revWalk.parseCommit(rootId);
					revWalk.markStart(root);

					String lmtSha = StringUtils.isNotEmpty(limitSha) ? limitSha : "";

					RevCommit c = revWalk.next();
					if (c != null) {
						while (c != null && !c.getName().equalsIgnoreCase(lmtSha)) {
							c = revWalk.next();
							if (c != null) {
								System.out.println(c.getName() + " - " + c.getFullMessage());
							}
						}
					}

					revWalk.dispose();
				}
			}
		} catch (IOException e) {
			throw new JGitInternalException(
					MessageFormat.format(JGitText.get().exceptionCaughtDuringExecutionOfMergeCommand, e), e);
		} finally {
			setCallable(true);
		}
		return Collections.emptyMap();
	}

	private Ref getUpstreamRef() throws IOException {
		Ref ref = null;
		if (StringUtils.isNotEmpty(upstream)) {
			ref = repo.exactRef(upstream);
		}
		return ref;
	}

	private Ref getHeadRef() throws IOException, NoHeadException {
		Ref headRef = null;
		if (StringUtils.isNotEmpty(head)) {
			headRef = repo.exactRef(Constants.HEAD);
		}
		if (headRef == null) {
			throw new NoHeadException(JGitText.get().noHEADExistsAndNoExplicitStartingRevisionWasSpecified);
		}
		return headRef;
	}

	public CherryCommand setUpstream(String upstream) {
		this.upstream = upstream;
		return this;
	}

	public CherryCommand setHead(String head) {
		this.head = head;
		return this;
	}

	public CherryCommand setLimit(String limitSha) {
		this.limitSha = limitSha;
		return this;
	}
}
