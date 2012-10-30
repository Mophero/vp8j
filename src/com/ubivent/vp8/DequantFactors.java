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

public class DequantFactors {
	public final static int TOKEN_BLOCK_Y1 = 0;
	public final static int TOKEN_BLOCK_UV = 1;
	public final static int TOKEN_BLOCK_Y2 = 2;
	public final static int TOKEN_BLOCK_TYPES = 3;
	   
    int   quant_idx;
    short factor[][] = new short[TOKEN_BLOCK_TYPES][2]; /* [ Y1, UV, Y2 ]
                                         * [ DC, AC ] */
}
