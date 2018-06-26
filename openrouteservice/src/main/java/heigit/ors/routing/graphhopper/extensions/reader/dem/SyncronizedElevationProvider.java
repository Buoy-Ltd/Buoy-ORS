/*
 *  Licensed to GIScience Research Group, Heidelberg University (GIScience)
 *
 *   http://www.giscience.uni-hd.de
 *   http://www.heigit.org
 *
 *  under one or more contributor license agreements. See the NOTICE file 
 *  distributed with this work for additional information regarding copyright 
 *  ownership. The GIScience licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in compliance 
 *  with the License. You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package heigit.ors.routing.graphhopper.extensions.reader.dem;

import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.storage.DAType;

import java.util.concurrent.locks.StampedLock;

public class SyncronizedElevationProvider implements ElevationProvider 
{
	private final StampedLock _lock = new StampedLock();
	private ElevationProvider _elevProvider;

	public SyncronizedElevationProvider(ElevationProvider elevProvider)
	{
		_elevProvider = elevProvider;
	}

	@Override
	public double getEle(double lat, double lon) {
        // MARQ24 removed the code that is is checking tile cache - since the complete
        // ElevationProvider Interface have been rolled back to the original gh
        // implementation - if CACHING would/could help, then this should be implemented
        // within gh :-)

        // MARQ24: the only noticeable difference that might could exist, is, that the
        // original code had checked the HeightTile if it's seaLevel... this might can
        // make a difference - but hopefully mtb-tours in the atlantic ocean will not be
        // that popular in the next century...
        /*if (dem.isSeaLevel())
            return 0;
        return dem.getHeight(lat, lon);*/

        return _elevProvider.getEle(lat, lon);
	}

	@Override
	public ElevationProvider setBaseURL(String baseURL) {
		return _elevProvider.setBaseURL(baseURL);
	}

	@Override
	public ElevationProvider setDAType(DAType daType) {
		return _elevProvider.setDAType(daType);
	}

	@Override
	public void setCalcMean(boolean calcMean) {
		_elevProvider.setCalcMean(calcMean);
	}

	public void release(boolean disposeInternal) {
	  if (disposeInternal)
		  _elevProvider.release();
	}

    @Override
    public void setAutoRemoveTemporaryFiles(boolean autoRemoveTemporary) {
        _elevProvider.setAutoRemoveTemporaryFiles(autoRemoveTemporary);
    }

	@Override
	public void release() {
		// omit calling release method of the internal provider since it is used by OSMReader
		//_elevProvider.release();		
	}
}
