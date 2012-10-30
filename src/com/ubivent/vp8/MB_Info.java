 /*
    * Copyright (c) 2012, ubivent GmbH, Thomas Butter, Oliver Seuffert
    * All right reserved.
    * 
    * Use of this source code is governed by a BSD-style license
    * that can be found in the LICENSE file in the root of the source
    * tree.
    * 
    * This software is based on RFC6386
    * 
    * Copyright (c) 2010, 2011, Google Inc.  All rights reserved.
    *
    * Use of this source code is governed by a BSD-style license
    * that can be found in the LICENSE file in the root of the source
    * tree.  An additional intellectual property rights grant can be
    * found in the file PATENTS.  All contributing project authors may
    * be found in the AUTHORS file in the root of the source tree.
    */

package com.ubivent.vp8;

public class MB_Info {
	class mv {
		int x;
		int y;
	}

	enum splitmv_partitioning {
		SPLITMV_16X8, SPLITMV_8X16, SPLITMV_8X8, SPLITMV_4X4
	};

	byte y_mode;
	byte uv_mode;
	byte segment_id;
	byte ref_frame;
	boolean skip_coeff;
	byte need_mc_border;
	splitmv_partitioning partitioning;
	mv mv;
	int eob_mask;
	mv mvs[] = new mv[16]; // in a union with enum prediction_mode modes[16];
	byte modes[] = new byte[16]; // TODO ???
}
