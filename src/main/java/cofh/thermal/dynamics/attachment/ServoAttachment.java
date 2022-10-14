package cofh.thermal.dynamics.attachment;

import cofh.core.util.filter.EmptyFilter;
import cofh.core.util.filter.IFilter;

public class ServoAttachment implements IFilterableAttachment {

    protected IFilter filter = EmptyFilter.INSTANCE;

    // region IFilterableAttachment
    @Override
    public IFilter getFilter() {

        return filter;
    }
    // endregion
}
