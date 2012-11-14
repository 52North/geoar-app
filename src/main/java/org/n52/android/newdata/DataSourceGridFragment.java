/**
 * Copyright 2012 52°North Initiative for Geospatial Open Source Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.android.newdata;

import java.util.List;

import org.n52.android.data.ImageLoader;
import org.n52.android.geoar.R;
import org.n52.android.newdata.DataSourceLoader.OnDataSourcesChangeListener;

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

public class DataSourceGridFragment extends Fragment {

	private GridView gridView;
	private GridAdapter gridAdapter;
	private ImageLoader imageLoader;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (getActivity() != null) {
			gridAdapter = new GridAdapter(getActivity());
			imageLoader = ImageLoader.getInstance();

			if (gridView != null) {
				gridView.setAdapter(gridAdapter);
			}

			gridView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					if (getActivity() != null) {
						DataSourceHolder selectedDataSource = gridAdapter
								.getItem(position);

						DataSourceDialogFragment
								.newInstance(selectedDataSource).show(
										getFragmentManager(), "DataSource");
					}
				}
			});
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.cb_grid_fragment, container,
				false);

		gridView = (GridView) view.findViewById(R.id.cb_grid_view);
		return view;
	}

	private class GridAdapter extends BaseAdapter implements
			OnDataSourcesChangeListener {

		private class ViewHolder {
			public ImageView imageView;
			public TextView textView;
		}

		private List<DataSourceHolder> dataSources;
		private LayoutInflater inflater;

		public GridAdapter(Context context) {
			dataSources = DataSourceLoader
					.getDataSources();
			inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			DataSourceLoader
					.addOnAvailableDataSourcesUpdateListener(this);
			// TODO remove..Listener?
		}

		@Override
		public int getCount() {
			if (dataSources != null)
				return dataSources.size();
			return 0;
		}

		@Override
		public DataSourceHolder getItem(int position) {
			if (dataSources != null && position < getCount() && position >= 0)
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

			if (view == null) {
				view = inflater.inflate(R.layout.cb_grid_item, parent, false);
				viewHolder = new ViewHolder();
				viewHolder.imageView = (ImageView) view
						.findViewById(R.id.cb_grid_image);
				viewHolder.textView = (TextView) view
						.findViewById(R.id.cb_grid_label);

				view.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) view.getTag();
			}

			DataSourceHolder dataSource = dataSources.get(position);
			// load image via imageCache
			imageLoader.displayImage("", viewHolder.imageView); // TODO
			viewHolder.textView.setText(dataSource.getName());

			return view;
		}

		@Override
		public void onDataSourcesChange() {
			notifyDataSetChanged();
		}
	}
}
