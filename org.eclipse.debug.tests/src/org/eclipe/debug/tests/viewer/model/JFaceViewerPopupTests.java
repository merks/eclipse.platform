/*******************************************************************************
 * Copyright (c) 2009, 2010 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipe.debug.tests.viewer.model;

import org.eclipse.debug.internal.ui.viewers.model.ITreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.PresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * @since 3.6
 */
public class JFaceViewerPopupTests extends PopupTests {
    
    public JFaceViewerPopupTests(String name) {
        super(name);
    }

    protected ITreeModelViewer createViewer(Display display, Shell shell, int style) {
        return new TreeModelViewer(fShell, SWT.VIRTUAL | style, new PresentationContext("TestViewer"));
    }
}
