/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 * yyyymmdd bug      Email and other contact information
 * -------- -------- -----------------------------------------------------------
 * 20060217   127138 pmoogk@ca.ibm.com - Peter Moogk
 * 20070201   154100 pmoogk@ca.ibm.com - Peter Moogk, Port internet code from WTP to Eclipse base.
 *******************************************************************************/

package org.eclipse.net.internal.ui;

import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * This class is the Composite that consists of the controls for
 * "http.nonProxyHosts" and is used by InternetPreferencesPage.
 */
public class NonProxyHostsComposite extends Composite 
{
	private Table       table_;
	TableViewer tableViewer_;
	private TreeSet     tableValues_;      
	
	private Button add_;
	private Button edit_;
	private Button remove_;
	
	public NonProxyHostsComposite(Composite parent, int style ) 
	{
		super(parent, style);	
		createWidgets();
	}
	 
  public void enableComposite( boolean enabled )
  {
    table_.setEnabled( enabled );
    add_.setEnabled( enabled );
    edit_.setEnabled( enabled );
    remove_.setEnabled( enabled );
  }
  
	protected void createWidgets() 
	{
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 6;
		layout.verticalSpacing = 6;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 2;
		setLayout(layout);
    		
		table_ = new Table(this, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
		
		table_.setLayoutData(data);
		table_.setHeaderVisible(false);
		table_.setLinesVisible(true);
		
		TableLayout tableLayout = new TableLayout();
		
		new TableColumn(table_, SWT.NONE);
		ColumnWeightData colData = new ColumnWeightData(100, 60, false);
		tableLayout.addColumnData(colData);
				
		table_.setLayout(tableLayout);
		
		tableViewer_ = new TableViewer(table_);
		tableViewer_.setContentProvider(new NonProxyHostsContentProvider());
		tableViewer_.setLabelProvider(new NonProxyHostsLabelProvider());
		
		tableViewer_.addSelectionChangedListener(new ISelectionChangedListener() 
    {
			public void selectionChanged(SelectionChangedEvent event) 
      {
				enableButtons();
			}
		});
		
		Composite buttonComp = new Composite(this, SWT.NONE);
		layout = new GridLayout();
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 8;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 1;
		buttonComp.setLayout(layout);
		data = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_FILL);
		buttonComp.setLayoutData(data);
		
		add_ = createButton( buttonComp, NetUIMessages.BUTTON_PREFERENCE_ADD );
    
		add_.addSelectionListener(new SelectionAdapter() 
		{
			public void widgetSelected(SelectionEvent e) 
			{
				InputDialog dialog = new InputDialog( getShell(), 
					NetUIMessages.TITLE_PREFERENCE_HOSTS_DIALOG,
					NetUIMessages.LABEL_PREFERENCE_HOSTS_DIALOG,
					"", //$NON-NLS-1$
					null );
            
				if (dialog.open() != Window.CANCEL) 
				{
					updateList( dialog.getValue() );
				}
			}
		});		
		
		edit_ = createButton(buttonComp, NetUIMessages.BUTTON_PREFERENCE_EDIT );
    
		edit_.addSelectionListener(new SelectionAdapter() 
		{
			public void widgetSelected(SelectionEvent e) 
			{		
				IStructuredSelection selection     = (IStructuredSelection)tableViewer_.getSelection();
				String               selectedHosts = getStringList( selection.iterator() );
        
				InputDialog dialog = new InputDialog( getShell(), 
					NetUIMessages.TITLE_PREFERENCE_HOSTS_DIALOG,
					NetUIMessages.LABEL_PREFERENCE_HOSTS_DIALOG,
					selectedHosts,
					null );
                                              
				if (dialog.open() != Window.CANCEL) 
				{
					removeFromList( selection );
					updateList( dialog.getValue() );
				}
			}
		});
		edit_.setEnabled(false);
		
		remove_ = createButton(buttonComp, NetUIMessages.BUTTON_PREFERENCE_REMOVE);
    
		remove_.addSelectionListener(new SelectionAdapter() 
		{
			public void widgetSelected(SelectionEvent e) 
			{
				removeFromList( (IStructuredSelection)tableViewer_.getSelection() );
				tableViewer_.refresh();
			}
		});
		remove_.setEnabled(false);		
	}
  
	private Button createButton(Composite comp, String label) 
	{
		Button button = new Button(comp, SWT.PUSH);
		button.setText(label);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING);
		button.setLayoutData(data);
		return button;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Control#setEnabled(boolean)
	 */  
	public void setEnabled(boolean enabled) 
	{
		super.setEnabled(enabled);
		enableButtons();
	}
  
	public void setList( String[] hosts )
	{
		tableValues_ = new TreeSet( Arrays.asList( hosts ) );
    
		tableViewer_.setInput( tableValues_ );
		tableViewer_.refresh();
	}
    
	public String[] getList()
	{
		return (String[])tableValues_.toArray( new String[0] );
	}
  
	String getStringList( Iterator iterator )
	{
		StringBuffer buffer   = new StringBuffer();
    
		if( iterator.hasNext() )
		{
			buffer.append( (String)iterator.next() );
		}
    
		while( iterator.hasNext() )
		{
			buffer.append( ',' );
			buffer.append( (String)iterator.next() );
		}
      
		return buffer.toString();
	}
  
	void removeFromList( IStructuredSelection selection )
	{
		tableValues_.removeAll( selection.toList() );
	}
  
	void updateList( String value )
	{
    // Split the string with a delimiter of either a vertical bar, a space,
    // or a comma.
    String[] hosts = value.split( "\\|| |," ); //$NON-NLS-1$
    
		tableValues_.addAll( Arrays.asList( hosts ) );
    tableValues_.remove( "" ); //$NON-NLS-1$
		tableViewer_.refresh();
	}
  
	void enableButtons()
	{
		boolean enabled = getEnabled();
    
		if( enabled )
		{
			boolean itemsSelected = !tableViewer_.getSelection().isEmpty();
      
			add_.setEnabled( true );
			edit_.setEnabled( itemsSelected );
			remove_.setEnabled( itemsSelected );
		}
		else
		{
			add_.setEnabled( false );
			edit_.setEnabled( false );
			remove_.setEnabled( false );
		}
	}
}
