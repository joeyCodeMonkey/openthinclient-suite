/*******************************************************************************
 * openthinclient.org ThinClient suite
 * 
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * 
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 ******************************************************************************/
package org.openthinclient.console;

import java.io.IOException;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openthinclient.common.model.Realm;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.Util;

/**
 * @author Michael Gold
 */
public class DeleteRealmAction extends NodeAction {
	/*
	 * @see org.openide.util.actions.CallableSystemAction#asynchronous()
	 */
	@Override
	protected boolean asynchronous() {
		return true;
	}

	/*
	 * @see org.openide.util.actions.NodeAction#performAction(org.openide.nodes.Node[])
	 */
	@Override
	protected void performAction(Node[] nodes) {

		boolean delete = false;
		boolean ask = true;
		if (nodes.length > 1) {
			if (DialogDisplayer.getDefault().notify(
					new NotifyDescriptor.Confirmation((Messages
							.getString("action.deleteReally.question.three")
							+ " " + nodes.length + " " + Messages
							.getString("action.deleteReally.question.four")),
							NotifyDescriptor.YES_NO_OPTION)) == NotifyDescriptor.YES_OPTION)
				delete = true;
			ask = false;
		}
		for (final Node node : nodes)
			if (node instanceof EditorProvider) {
				final Realm realm = (Realm) node.getLookup().lookup(Realm.class);
				final LDAPConnectionDescriptor lcd = realm.getConnectionDescriptor();
				try {
					if (ask == true)
						if (DialogDisplayer.getDefault().notify(
								new NotifyDescriptor.Confirmation((Messages
										.getString("action.deleteReally.question.one")
										+ " " + realm.getName() + " " + Messages
										.getString("action.deleteReally.question.two")),
										NotifyDescriptor.YES_NO_OPTION)) == NotifyDescriptor.YES_OPTION)
							delete = true;

					if (delete == true) {
						final DirContext ctx = lcd.createDirContext();
						try {
							Util.deleteRecursively(ctx, Util.makeRelativeName("", lcd));

							try {
								node.destroy();
							} catch (final IOException e) {
								e.printStackTrace();
							}
						} finally {
							ctx.close();
						}
					}

				} catch (final NamingException e) {
					e.printStackTrace();
				}
			}
	}

	/*
	 * @see org.openide.util.actions.NodeAction#enable(org.openide.nodes.Node[])
	 */
	@Override
	protected boolean enable(Node[] arg0) {
		return true;
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getName()
	 */
	@Override
	public String getName() {
		return Messages.getString("action." + this.getClass().getSimpleName()); //$NON-NLS-1$
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getHelpCtx()
	 */
	@Override
	public HelpCtx getHelpCtx() {
		return null;
	}
}
