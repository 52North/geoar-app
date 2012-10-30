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
package org.n52.android;

import java.util.ArrayList;

import org.n52.android.ext.actionbar.ActionBarActivity;
import org.n52.android.geoar.R;
import org.n52.android.newdata.DataSourceGridFragment;
import org.n52.android.newdata.DataSourceLoader;
import org.n52.android.newdata.DataSourceLoader.OnDataSourcesUpdateListener;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TabWidget;

/**
 * 
 * @author Arne de Wall
 * 
 */
public class MainActivity2 extends ActionBarActivity {

	TabHost tabHost;
	ViewPager viewPager;
	CBTabsAdapter tabsAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_tabs_pager);

		viewPager = (ViewPager) findViewById(R.id.pager);
		viewPager.setDrawingCacheEnabled(true);

		tabHost = (TabHost) findViewById(android.R.id.tabhost);
		tabHost.setup();

		tabsAdapter = new CBTabsAdapter(this, viewPager, tabHost);
		tabsAdapter.addtab(
				tabHost.newTabSpec("Installed New").setIndicator(
						"Installed new"), DataSourceGridFragment.class, null);

//		tabsAdapter.addtab(
//				tabHost.newTabSpec("Codebase").setIndicator("Codebase"),
//				CodebaseGridFragment.class, null);
//		tabsAdapter.addtab(
//				tabHost.newTabSpec("Installed").setIndicator("Installed"),
//				DatasourceGridFragment.class, null);

		if (savedInstanceState != null)
			tabHost.setCurrentTabByTag(savedInstanceState.getString("tabState"));
		else
			//DataSourceAdapter.initFactoryLoader(getClassLoader(), this);

		// Test!
		DataSourceLoader.getInstance().addOnDataSourcesUpdateListener(
				new OnDataSourcesUpdateListener() {
					@Override
					public void onDataSourcesUpdate() {
						Intent intent = new Intent(MainActivity2.this
								.getApplicationContext(), GeoARActivity3.class);
						MainActivity2.this.startActivity(intent);
					}
				});
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("tabState", tabHost.getCurrentTabTag());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.n52.android.ext.actionbar.ActionBarActivity#onCreateOptionsMenu(android
	 * .view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.cb_main_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.cb_menu_about:
			AboutDialog aboutDialog = new AboutDialog(this);
			aboutDialog.setTitle(R.string.about_titel);
			aboutDialog.show();
			return true;
		case R.id.cb_menu_refresh:
			DataSourceLoader.getInstance().reloadPlugins();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}

	/**
	 * It relies on a trick presented on
	 * http://developer.android.com/resources/samples
	 * /Support4Demos/src/com/example
	 * /android/supportv4/app/FragmentTabsPager.html
	 */
	public static class CBTabsAdapter extends FragmentPagerAdapter implements
			ViewPager.OnPageChangeListener, TabHost.OnTabChangeListener {

		static final class CBTabInfo {
			private final String thisTag;
			private final Class<?> thisFragmentClass;
			private final Bundle thisArgs;

			CBTabInfo(String tag, Class<?> fragmentClass, Bundle args) {
				thisFragmentClass = fragmentClass;
				thisArgs = args;
				thisTag = tag;
			}
		}

		static class DummyTabFactory implements TabHost.TabContentFactory {
			private final Context context;

			public DummyTabFactory(Context context) {
				this.context = context;
			}

			@Override
			public View createTabContent(String tag) {
				View view = new View(context);
				view.setMinimumHeight(0);
				view.setMinimumWidth(0);
				return view;
			}
		}

		private ArrayList<CBTabInfo> tabs = new ArrayList<CBTabInfo>();
		private TabHost tabHost;
		private Context context;
		private ViewPager viewPager;

		public CBTabsAdapter(FragmentManager fm) {
			super(fm);
		}

		public CBTabsAdapter(FragmentActivity context, ViewPager viewPager,
				TabHost tabHost) {
			super(context.getSupportFragmentManager());
			this.tabHost = tabHost;
			this.viewPager = viewPager;
			this.context = context;

			viewPager.setAdapter(this);
			viewPager.setOnPageChangeListener(this);

			tabHost.setOnTabChangedListener(this);
		}

		@Override
		public void onTabChanged(String tabId) {
			viewPager.setCurrentItem(tabHost.getCurrentTab());
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
			// nothing
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
			// nothing
		}

		@Override
		public void onPageSelected(int arg0) {
			TabWidget widget = tabHost.getTabWidget();
			int dfocus = widget.getDescendantFocusability();
			widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
			tabHost.setCurrentTab(arg0);
			widget.setDescendantFocusability(dfocus);
		}

		@Override
		public Fragment getItem(int arg0) {
			CBTabInfo tabInfo = tabs.get(arg0);
			return Fragment.instantiate(context,
					tabInfo.thisFragmentClass.getName(), tabInfo.thisArgs);
		}

		@Override
		public int getCount() {
			return tabs.size();
		}

		public void addtab(TabHost.TabSpec tabSpec, Class<?> fragmentClass,
				Bundle args) {
			tabSpec.setContent(new DummyTabFactory(context));
			CBTabInfo tabInfo = new CBTabInfo(tabSpec.getTag(), fragmentClass,
					args);
			tabs.add(tabInfo);
			tabHost.addTab(tabSpec);
			notifyDataSetChanged();
		}

	}

}
