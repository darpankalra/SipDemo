package com.example.android.sip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class History extends ListActivity {
	
	private String FILENAME="Hist_file";
	private ArrayList<CallInfo> s=new ArrayList<CallInfo>();
	FileInputStream fis;
	ObjectInputStream ois;
	CallInfoAdapter cia;
	public final static String ADDRTOCALL="com.example.android.sip.ADDRTOCALL";
	boolean callConnected;
	static final int CALL_CONNECTED_DIALOG=0;
	AdapterContextMenuInfo info;
	private EditText filterText = null;
	
	private static final int CLEAR_LOG = 1;
	private static final int SEARCH = 2;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.history_list);
		retrieveHistory();
/*		ListAdapter adapter=new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, s);
		setListAdapter(adapter);*/
		
		filterText = (EditText) findViewById(R.id.search_box);
		filterText.addTextChangedListener(filterTextWatcher);
		
		cia=new CallInfoAdapter(s);
		setListAdapter(cia);
		registerForContextMenu(getListView());
		Intent intent=getIntent();
		callConnected=intent.getBooleanExtra(WalkieTalkieActivity.CALLSTATUS, false);
		
//		Toast.makeText(this, ""+filterText.getText(), Toast.LENGTH_LONG).show();
//		Log.d("call checking", ""+callConnected);
	}
	
	private TextWatcher filterTextWatcher = new TextWatcher() {
		
//		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			// TODO Auto-generated method stub
			
		}
		
//		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// TODO Auto-generated method stub
			
		}
		
//		@Override
		public void afterTextChanged(Editable s) {
			// TODO Auto-generated method stub
			if(filterText.getText()==null || filterText.getText().equals(""))
			{
				cia.notifyDataSetChanged();
			}
			else
				cia.getFilter().filter(s);
		}
	};
	
	public void retrieveHistory()
	{
		try
		{
			fis=openFileInput(FILENAME);
			ois=new ObjectInputStream(fis);
			CallInfo cinfo;
			while((cinfo=(CallInfo)ois.readObject())!=null)
			{
				s.add(cinfo);
			}
		}
		catch(Exception e)
		{
//			Toast toast=Toast.makeText(getApplicationContext(), "Unable to read data from file "+e, Toast.LENGTH_LONG);
//            toast.show();
			try{
			ois.close();}
			catch(Exception ex)
			{
//				Toast toast2=Toast.makeText(getApplicationContext(), "Unable to read data from file "+ex, Toast.LENGTH_LONG);
//	            toast2.show();
			}
		}
		Collections.reverse(s);
	}
	
	@Override
	public void onListItemClick(ListView parent, View v, int position, long id)
	{
		openContextMenu(v);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater=getMenuInflater();
		inflater.inflate(R.menu.context_menu, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		info = (AdapterContextMenuInfo)item.getMenuInfo();
		switch(item.getItemId())
		{
		case R.id.callitem:
			if(callConnected)
			{
				showDialog(CALL_CONNECTED_DIALOG);
			}
			else
			{
				WalkieTalkieActivity.callSomeone=true;
//				Toast.makeText(this, ""+s.get(info.position), Toast.LENGTH_LONG).show();
				Intent intent=new Intent(this,WalkieTalkieActivity.class);
				intent.putExtra(ADDRTOCALL, "sip:"+String.valueOf(s.get(info.position)));
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startActivity(intent);
			}
			return true;
		case R.id.deleteitem:
//			Toast.makeText(this, ""+s.get(info.position)+"  "+info.id, Toast.LENGTH_LONG).show();
			s.remove(info.position);
			cia.notifyDataSetChanged();
			try
			{
				int i=s.size()-1;
				File oldfile=new File("/data/data/com.example.android.sip/files/"+FILENAME);
				File newfile=new File("/data/data/com.example.android.sip/files/Hist_file2");
				FileOutputStream fos = openFileOutput("Hist_file2", Context.MODE_APPEND);
				ObjectOutputStream oos=new ObjectOutputStream(fos);
				for(;i>=0;i--)
				{
					oos.writeObject(s.get(i));
				}
				oos.flush();
				fos.close();
				oos.close();
				boolean deleted=oldfile.delete();
				if(deleted)
				{
					boolean renamed=newfile.renameTo(oldfile);
					if(renamed)
					{
						Toast.makeText(this, "Record deleted successfully", Toast.LENGTH_LONG).show();
					}
				}
			}
			catch(Exception e)
			{
				Toast.makeText(this, "Unable to delete "+e, Toast.LENGTH_LONG).show();
			}
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}
	
	@Override
	public Dialog onCreateDialog(int id)
	{
		AlertDialog dialog=null;
		switch(id)
		{
		case CALL_CONNECTED_DIALOG:
			AlertDialog.Builder builder=new AlertDialog.Builder(this);
			builder.setMessage("You are currently in call.\nIf you select this option your current call will be disconnected")
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					
//					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						WalkieTalkieActivity.endCall=true;
						WalkieTalkieActivity.callSomeone=true;
//						Toast.makeText(this, ""+s.get(info.position), Toast.LENGTH_LONG).show();
						Intent intent=new Intent(getBaseContext(),WalkieTalkieActivity.class);
						intent.putExtra(ADDRTOCALL, "sip:"+String.valueOf(s.get(info.position)));
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
						startActivity(intent);
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					
//					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						
					}
				})
				.setCancelable(false);
			dialog=builder.create();
			break;
		case CLEAR_LOG:
			AlertDialog.Builder builder2=new AlertDialog.Builder(this);
			builder2.setMessage("Are you sure you want to delete all recent calls?")
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					
//					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						File f=new File("/data/data/com.example.android.sip/files/"+FILENAME);
						boolean b=f.delete();
						if(b)
						{
							Toast.makeText(getApplicationContext(), "Before delete", Toast.LENGTH_SHORT).show();
							s.clear();
							cia.notifyDataSetChanged();
							Toast.makeText(getApplicationContext(), "After delete", Toast.LENGTH_SHORT).show();
						}
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					
//					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						
					}
				})
				.setCancelable(false);
			dialog=builder2.create();
			break;
		}
		return dialog;
	}
	
	class CallInfoAdapter extends ArrayAdapter<CallInfo>
	{
		public CallInfoAdapter(ArrayList<CallInfo> list) 
		{
			super(History.this,R.layout.history_row,R.id.sipaddr, list);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View row=super.getView(position, convertView, parent);
			if(row==null)
			{
				LayoutInflater inflater=getLayoutInflater();
				row=inflater.inflate(R.layout.history_row, parent, false);
			}
			
			TextView sipaddr = (TextView) row.findViewById(R.id.sipaddr);
			sipaddr.setText(s.get(position).getSipAddr());
			
			TextView calldate = (TextView) row.findViewById(R.id.calldate);
			calldate.setText(s.get(position).getCallDate());
			
			TextView callduration = (TextView) row.findViewById(R.id.callduration);
			callduration.setText(s.get(position).getCallDuration());
			
			ImageView icon = (ImageView) row.findViewById(R.id.icon);
			if(s.get(position).isOutgoingCall())
				icon.setImageResource(R.drawable.out_call);
			else
				icon.setImageResource(R.drawable.in_call);
			if(s.get(position).isMissedCall())
				icon.setImageResource(R.drawable.miss_call);
			
			return row;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, CLEAR_LOG, 0, "Clear Log").setIcon(R.drawable.trash);
		menu.add(0, SEARCH, 0, "Search").setIcon(R.drawable.actionsearch);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
		case CLEAR_LOG:
			showDialog(CLEAR_LOG);
			break;
		case SEARCH:
			filterText = (EditText) findViewById(R.id.search_box);
			filterText.setVisibility(View.VISIBLE);
//			findViewById(android.R.id.list).setEnabled(false);
//			findViewById(android.R.id.list).setFocusable(false);
			break;
		}
		return true;
	}
}
