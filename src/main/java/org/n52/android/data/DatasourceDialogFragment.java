package org.n52.android.data;

import org.n52.android.data.CodebaseGridFragment.GridChangedCallback;
import org.n52.android.geoar.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class DatasourceDialogFragment extends DialogFragment {

	static DatasourceDialogFragment newInstance(String id, String datasourceName, String description, boolean codebase, GridChangedCallback callback){
		DatasourceDialogFragment df = new DatasourceDialogFragment();
		Bundle args = new Bundle();
		args.putString("id", id);
		args.putString("name", datasourceName);
		args.putString("description", description);
		args.putBoolean("isCodebase", codebase);
		args.putSerializable("callback", callback);
		df.setArguments(args);
		return df;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// get paramters
		Bundle args = getArguments();
		final String id 			= args.getString("id");
		final String name 			= args.getString("name");
		final String description 	= args.getString("description");
		final boolean isCodebase 	= args.getBoolean("isCodebase");
		final GridChangedCallback c = (GridChangedCallback) args.getSerializable("callback");
		
		// inflate layout
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View v = inflater.inflate(R.layout.cb_dialog_fragment, null);

		TextView textView = (TextView) v.findViewById(R.id.cb_dialog_textview);
		textView.setText(description);
		
		ImageView imageView = (ImageView) v.findViewById(R.id.cb_dialog_image);
		ImageLoader.getInstance().displayImage(id, imageView);
//		imageView.setImageBitmap();
		
//		dialogButton.setAnimation(getActivity().findViewById(android.R.drawable.stat_sys_download));
		Dialog dsDialog = new AlertDialog.Builder(getActivity())
							.setTitle(name)
							.setPositiveButton((!isCodebase) ? "Download" : "Start", new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									if(isCodebase){
										DataSourceAdapter.startDataSource(id);
									}
									else{
										c.setProgressUpdater();
									}
								}
							})
							.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
									
								}
							})
							.setView(v)
							.create();
		
		return dsDialog;
	}
	
}
