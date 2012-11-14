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
package org.n52.android.view;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class GeoARViewPager extends ViewPager {

	private List<Fragment> fragments = new ArrayList<Fragment>();
	private GeoARPagerAdpter mPagerAdapter;

	public GeoARViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public GeoARViewPager(Context context) {
		super(context);
	}

	public void setFragmentManager(FragmentManager fm) {
		mPagerAdapter = new GeoARPagerAdpter(fm);
		setAdapter(mPagerAdapter);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		return false;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		return false;
	}

	public void showFragment(Fragment fragment) {
		setCurrentItem(fragments.indexOf(fragment));
	}

	public Fragment getCurrentFragment() {
		return fragments.get(getCurrentItem());
	}

	public void addFragment(Fragment fragment) {
		fragments.add(fragment);
	}

	private class GeoARPagerAdpter extends FragmentStatePagerAdapter {

		public GeoARPagerAdpter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			return fragments.get(position);
		}

		@Override
		public int getCount() {
			return fragments.size();
		}

	}
}
