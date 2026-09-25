package com.epic60869.skyjew.sb.utils.render;

import com.epic60869.skyjew.sb.utils.render.primitive.PrimitiveCollector;

public interface Renderable {
	void extractRendering(PrimitiveCollector collector);
}
