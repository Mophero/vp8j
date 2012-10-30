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

public class DequantData {
	static final int dc_q_lookup[] =
		   {
		       4,    5,    6,    7,    8,    9,    10,   10,
		       11,   12,   13,   14,   15,   16,   17,   17,
		       18,   19,   20,   20,   21,   21,   22,   22,
		       23,   23,   24,   25,   25,   26,   27,   28,
		       29,   30,   31,   32,   33,   34,   35,   36,
		       37,   37,   38,   39,   40,   41,   42,   43,
		       44,   45,   46,   46,   47,   48,   49,   50,
		       51,   52,   53,   54,   55,   56,   57,   58,
		       59,   60,   61,   62,   63,   64,   65,   66,
		       67,   68,   69,   70,   71,   72,   73,   74,
		       75,   76,   76,   77,   78,   79,   80,   81,
		       82,   83,   84,   85,   86,   87,   88,   89,
		       91,   93,   95,   96,   98,   100,  101,  102,
		       104,  106,  108,  110,  112,  114,  116,  118,
		       122,  124,  126,  128,  130,  132,  134,  136,
		       138,  140,  143,  145,  148,  151,  154,  157
		   };

		   static final int ac_q_lookup[] =
		   {
		       4,    5,    6,    7,    8,    9,    10,   11,
		       12,   13,   14,   15,   16,   17,   18,   19,
		       20,   21,   22,   23,   24,   25,   26,   27,
		       28,   29,   30,   31,   32,   33,   34,   35,
		       36,   37,   38,   39,   40,   41,   42,   43,
		       44,   45,   46,   47,   48,   49,   50,   51,
		       52,   53,   54,   55,   56,   57,   58,   60,
		       62,   64,   66,   68,   70,   72,   74,   76,
		       78,   80,   82,   84,   86,   88,   90,   92,
		       94,   96,   98,   100,  102,  104,  106,  108,
		       110,  112,  114,  116,  119,  122,  125,  128,
		       131,  134,  137,  140,  143,  146,  149,  152,
		       155,  158,  161,  164,  167,  170,  173,  177,
		       181,  185,  189,  193,  197,  201,  205,  209,
		       213,  217,  221,  225,  229,  234,  239,  245,
		       249,  254,  259,  264,  269,  274,  279,  284
		   };

}
