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
    * tree.  An additional byteellectual property rights grant can be
    * found in the file PATENTS.  All contributing project authors may
    * be found in the AUTHORS file in the root of the source tree.
    */

package com.ubivent.vp8;

public class PredictionMode {
	/* 16x16 bytera modes */
	static final byte DC_PRED = 0;
	static final byte V_PRED = 1;
	static final byte H_PRED = 2;
	static final byte TM_PRED = 3;
	static final byte B_PRED = 4;

	/* 16x16 byteer modes */
	static final byte NEARESTMV = 5;
	static final byte NEARMV = 6;
	static final byte ZEROMV = 7;
	static final byte NEWMV = 8;
	static final byte SPLITMV = 9;

	static final byte MB_MODE_COUNT = 10;

	/* 4x4 bytera modes */
	static final byte B_DC_PRED = 0;
	static final byte B_TM_PRED = 1;
	static final byte B_VE_PRED = 2;
	static final byte B_HE_PRED = 3;
	static final byte B_LD_PRED = 4;
	static final byte B_RD_PRED = 5;
	static final byte B_VR_PRED = 6;
	static final byte B_VL_PRED = 7;
	static final byte B_HD_PRED = 8;
	static final byte B_HU_PRED = 9;

	/* 4x4 byteer modes */
	static final byte LEFT4X4 = 10;
	static final byte ABOVE4X4 = 11;
	static final byte ZERO4X4 = 12;
	static final byte NEW4X4 = 13;

	static final byte B_MODE_COUNT = 14;
}
