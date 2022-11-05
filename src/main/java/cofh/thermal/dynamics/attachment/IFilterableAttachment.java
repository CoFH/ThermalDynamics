package cofh.thermal.dynamics.attachment;

import cofh.core.util.filter.IFilter;

public interface IFilterableAttachment extends IAttachment {

    IFilter getFilter();

}
