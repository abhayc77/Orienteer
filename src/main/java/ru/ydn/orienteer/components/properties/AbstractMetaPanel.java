package ru.ydn.orienteer.components.properties;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.markup.IMarkupFragment;
import org.apache.wicket.markup.html.form.ILabelProvider;
import org.apache.wicket.markup.html.form.LabeledWebMarkupContainer;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;

import ru.ydn.orienteer.components.IMetaComponentResolver;
import ru.ydn.orienteer.services.IMarkupProvider;

import com.google.inject.Inject;

public abstract class AbstractMetaPanel<T, C, V> extends GenericPanel<V> implements ILabelProvider<String>
{


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String PANEL_ID = "panel";
	
	private Serializable stateSignature;
	private  IModel<C> criteryModel;
	
	private IModel<String> labelModel;
	
	@Inject
	private IMarkupProvider markupProvider;
	
	public AbstractMetaPanel(String id, IModel<C> criteryModel, IModel<V> model) {
		super(id, model);
		this.criteryModel = criteryModel;
	}

	public AbstractMetaPanel(String id, IModel<C> criteryModel) {
		super(id);
		this.criteryModel = criteryModel;
	}
	
	@Override
	protected void onConfigure() {
		super.onConfigure();
		C critery = criteryModel.getObject();
		IMetaComponentResolver<C> resolver = getComponentResolver();
		Serializable newSignature = subSign(resolver.getSignature(critery));
		if(!newSignature.equals(stateSignature) || get(PANEL_ID)==null)
		{
			stateSignature = newSignature;
			Component component = resolver.resolve(PANEL_ID, critery);
			if(component instanceof LabeledWebMarkupContainer)
			{
				((LabeledWebMarkupContainer)component).setLabel(getLabel());
			}
			addOrReplace(component);
		}
	}
	
	protected Serializable subSign(Serializable thisSignature)
	{
		return thisSignature;
	}
	
	public IModel<C> getCriteryModel() {
		return criteryModel;
	}
	
	public C getCriteryObject()
	{
		return getCriteryModel().getObject();
	}

	@Override
	public IMarkupFragment getMarkup(Component child) {
		if(child==null) return super.getMarkup(child);
		IMarkupFragment ret = markupProvider.provideMarkup(child);
		return ret!=null?ret:super.getMarkup(child);
	}
	
	@Override
	public IModel<String> getLabel() {
		if(labelModel==null)
		{
			labelModel = newLabelModel();
		}
		return labelModel;
	}

	protected abstract IModel<String> newLabelModel();
	protected abstract IMetaComponentResolver<C> getComponentResolver();
	
}