/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.help.ui.internal.views;

import org.eclipse.help.IHelpResource;
import org.eclipse.help.internal.search.federated.ISearchEngineResult;
import org.eclipse.jface.viewers.*;

public class FederatedSearchSorter extends ViewerSorter {
	/**
	 * @see org.eclipse.jface.viewers.ViewerSorter#compare(org.eclipse.jface.viewers.Viewer,java.lang.Object,java.lang.Object)
	 */
	public int compare(Viewer viewer, Object e1, Object e2) {
		try {
			ISearchEngineResult r1 = (ISearchEngineResult) e1;
			ISearchEngineResult r2 = (ISearchEngineResult) e2;
			IHelpResource c1 = r1.getCategory();
			IHelpResource c2 = r2.getCategory();
			if (c1!=null && c2!=null) {
				int cat = compare(viewer, r1.getCategory(), r2.getCategory());
				if (cat!=0) return cat;
			}
			float rank1 = ((ISearchEngineResult) e1).getScore();
			float rank2 = ((ISearchEngineResult) e2).getScore();
			if (rank1 - rank2 > 0) {
				return -1;
			} else if (rank1 == rank2) {
				return 0;
			} else {
				return 1;
			}
		} catch (Exception e) {
		}
		return super.compare(viewer, e1, e2);
	}
}