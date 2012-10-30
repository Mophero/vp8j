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

public class SegmentHeader {
	boolean enabled = false;
	boolean update_data = false;
	boolean update_map = false;
	boolean abs = false; /* 0=deltas, 1=absolute values */
	int tree_probs[] = new int[VP8Decoder.MB_FEATURE_TREE_PROBS];
	int lf_level[] = new int[VP8Decoder.MAX_MB_SEGMENTS];
	int quant_idx[] = new int[VP8Decoder.MAX_MB_SEGMENTS];
}
