package com.epic60869.skyballs.sb.utils.render;

import com.epic60869.skyballs.sb.utils.render.primitive.PrimitiveCollector;

public interface Renderable {
	void extractRendering(PrimitiveCollector collector);
}
