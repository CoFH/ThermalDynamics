package cofh.thermal.dynamics.common.attachment;

import cofh.core.util.filter.IFilter;

public interface IFilterableAttachment extends IAttachment {

    IFilter getFilter();

}
