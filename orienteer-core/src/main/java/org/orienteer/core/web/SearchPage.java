package org.orienteer.core.web;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import com.google.inject.Inject;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.Strings;
import org.orienteer.core.MountPath;
import org.orienteer.core.component.TabsPanel;
import org.orienteer.core.component.command.EditODocumentsCommand;
import org.orienteer.core.component.command.SaveODocumentsCommand;
import org.orienteer.core.component.property.DisplayMode;
import org.orienteer.core.component.table.OrienteerDataTable;
import org.orienteer.core.component.table.component.GenericTablePanel;
import org.orienteer.core.service.IOClassIntrospector;
import org.orienteer.core.widget.support.jquery.JQueryDashboardSupport;
import ru.ydn.wicket.wicketorientdb.model.OClassModel;
import ru.ydn.wicket.wicketorientdb.model.OClassNamingModel;
import ru.ydn.wicket.wicketorientdb.model.OQueryDataProvider;

import java.util.List;

/**
 * Page to search and display search results
 */
@MountPath("/search")
public class SearchPage extends OrienteerBasePage<String>
{
	public final static Ordering<OClass> CLASSES_ORDERING = Ordering.natural().nullsFirst().onResultOf(new Function<OClass, String>() {

		@Override
		public String apply(OClass input) {
			return new OClassNamingModel(input).getObject();
		}
	});
	private WebMarkupContainer resultsContainer;
	private IModel<OClass> selectedClassModel;
	
	@Inject
	private IOClassIntrospector oClassIntrospector;
	
	public SearchPage()
	{
		super(Model.of(""));
	}

	public SearchPage(IModel<String> model)
	{
		super(model);
	}

	public SearchPage(PageParameters parameters)
	{
		super(parameters);
	}

	@Override
	protected IModel<String> resolveByPageParameters(
			PageParameters pageParameters) {
		String query = pageParameters.get("q").toOptionalString();
		return Model.of(query);
	}
	
	public List<OClass> getClasses()
	{
		return CLASSES_ORDERING.sortedCopy(getDatabase().getMetadata().getSchema().getClasses());
	}

	@Override
	public void initialize() {
		super.initialize();
		selectedClassModel = new OClassModel(getClasses().get(0));
		
		Form<String> form = new Form<String>("form", getModel());
		TextField<String> field = new TextField<>("query", getModel());
		field.add(AttributeModifier.replace("placeholder", new ResourceModel("page.search.placeholder").getObject()));
		form.add(field);
		form.add(new AjaxButton("search") {

			@Override
			protected void onSubmit(AjaxRequestTarget target) {
				super.onSubmit(target);
				target.add(resultsContainer);
			}
		});
		
		form.add(new TabsPanel<OClass>("tabs", selectedClassModel, new PropertyModel<List<OClass>>(this, "classes"))
				{

					@Override
					public void onTabClick(AjaxRequestTarget target) {
						prepareResults();
						target.add(resultsContainer);
					}
			
				});
		
		resultsContainer = new WebMarkupContainer("resultsContainer")
		{
			{
				setOutputMarkupPlaceholderTag(true);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!Strings.isEmpty(SearchPage.this.getModelObject()));
			}
			
		};
		
		prepareResults();
		form.add(resultsContainer);
		add(form);
	}
	
	private void prepareResults()
	{
		prepareResults(selectedClassModel.getObject());
	}
	
	private void prepareResults(OClass oClass)
	{
		OQueryDataProvider<ODocument> provider = oClassIntrospector.getDataProviderForGenericSearch(oClass, getModel());
		oClassIntrospector.defineDefaultSorting(provider, oClass);
		IModel<DisplayMode> modeModel = DisplayMode.VIEW.asModel();
		GenericTablePanel<ODocument> tablePanel =
				new GenericTablePanel<ODocument>("results", oClassIntrospector.getColumnsFor(oClass, false, modeModel), provider, 20);
		OrienteerDataTable<ODocument, String> table =  tablePanel.getDataTable();
		table.addCommand(new EditODocumentsCommand(table, modeModel, oClass));
		table.addCommand(new SaveODocumentsCommand(table, modeModel));
		resultsContainer.addOrReplace(tablePanel);
	}

	@Override
	public IModel<String> getTitleModel() {
		return new ResourceModel("search.title");
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(new PriorityHeaderItem(JavaScriptHeaderItem.forReference(JQueryDashboardSupport.JQUERY_UI_JS)));
	}
}
