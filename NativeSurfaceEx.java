/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.leeho.filament.swt;

import com.google.android.filament.NativeSurface;

/**
 * extends {@link NativeSurface} for use swt native view id
 * @author LeeHo
 *
 */
public class NativeSurfaceEx extends NativeSurface {
	private final long mNativeObject;

	public NativeSurfaceEx(long nativeObject, int width, int height) {
		super(width, height);
		// dispose super class created native object
		super.dispose();
		mNativeObject = nativeObject;
	}

	public void dispose() {
		// native dispose ,because mNativeObject is a swt view id
	}

	public long getNativeObject() {
		return mNativeObject;
	}

}