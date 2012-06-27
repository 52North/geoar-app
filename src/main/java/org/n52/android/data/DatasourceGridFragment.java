package org.n52.android.data;

import java.util.ArrayList;
import java.util.List;

import org.n52.android.data.FactoryLoader.DatasourceHolder;
import org.n52.android.data.PluginLoader.PluginHolder;
import org.n52.android.data.PluginLoader.PluginUpdateListener;
import org.n52.android.geoar.R;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class DatasourceGridFragment extends Fragment{

	private GridView gridView;
	private GridAdapter gridAdapter;
	private ImageLoader imageLoader;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if(getActivity() != null){
			gridAdapter = new GridAdapter(getActivity());
			imageLoader = ImageLoader.getInstance();
			
			if(gridView != null){
				gridView.setAdapter(gridAdapter); 
			}
			
			gridView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					// TODO Auto-generated method stub
					if(getActivity()!= null){
						DatasourceHolder datasourceName = (DatasourceHolder) gridAdapter.getItem(position);
						DatasourceDialogFragment dialogFragment = DatasourceDialogFragment
								.newInstance(datasourceName.identification, datasourceName.description, "",  true, null);
						dialogFragment.show(getFragmentManager(), "Datasource");
						Toast.makeText(getActivity(), datasourceName.identification, Toast.LENGTH_SHORT).show();
					}
				}
			});
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.cb_grid_fragment, container, false);
		
		gridView = (GridView) view.findViewById(R.id.cb_grid_view);
		return view;
	}
	
	
	private class GridAdapter extends BaseAdapter implements PluginUpdateListener{
		
		private class ViewHolder{
			public ImageView imageView;
			public TextView textView;
		}
		
		private List<DatasourceHolder> dataSources;
		private LayoutInflater inflater;
		
		public GridAdapter(Context context){
			dataSources = new ArrayList<FactoryLoader.DatasourceHolder>();
			PluginLoader.addPluginUpdateListener(this, DatasourceHolder.class);
			DataSourceAdapter.refreshPluginLoader();
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			if(dataSources != null)
				return dataSources.size();
			return 0;
		}

		@Override
		public Object getItem(int position) {
			if(dataSources != null && position < getCount() && position >= 0)
				return dataSources.get(position);
			return null;
		}

		@Override
		public long getItemId(int position) {			
			return 0;
		}

		@Override
		public View getView(int position, View cView, ViewGroup parent) {
			View view = cView;
			ViewHolder viewHolder;
			
			if(view == null){
				view = inflater.inflate(R.layout.cb_grid_item, parent, false);
				viewHolder = new ViewHolder();
				viewHolder.imageView = (ImageView) view.findViewById(R.id.cb_grid_image);
				viewHolder.textView = (TextView) view.findViewById(R.id.cb_grid_label);
				
				view.setTag(viewHolder);
			}
			else{
				viewHolder = (ViewHolder) view.getTag();
			}
			
			DatasourceHolder dataSource = dataSources.get(position);
			// load image via imageCache
			imageLoader.displayImage(dataSource.identification, viewHolder.imageView);
			viewHolder.textView.setText(dataSource.description);
			
			return view;
		}

		@Override
		public void pluginUpdate(PluginHolder holder) {
			this.dataSources.add((DatasourceHolder) holder);
			notifyDataSetChanged();
		}

		@Override
		public void refreshViewOnMainThread() {
			notifyDataSetChanged();
		}
		
	}
}
