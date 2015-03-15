package org.apache.nifi.processors.solr;

import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.AllowableValue;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A base class for processors that interact with Apache Solr.
 *
 * @author bbende
 */
public abstract class SolrProcessor extends AbstractProcessor {

    public static final AllowableValue SOLR_TYPE_CLOUD = new AllowableValue(
            "Cloud", "Cloud", "A SolrCloud instance.");

    public static final AllowableValue SOLR_TYPE_STANDARD = new AllowableValue(
            "Standard", "Standard", "A stand-alone Solr instance.");

    public static final PropertyDescriptor SOLR_TYPE = new PropertyDescriptor
            .Builder().name("Solr Type")
            .description("The type of Solr instance, Cloud or Standard.")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .allowableValues(SOLR_TYPE_CLOUD, SOLR_TYPE_STANDARD)
            .defaultValue(SOLR_TYPE_STANDARD.getValue())
            .build();

    public static final PropertyDescriptor SOLR_LOCATION = new PropertyDescriptor
            .Builder().name("Solr Location")
            .description("The Solr url for a Solr Type of Standard, " +
                    "or the ZooKeeper hosts for a Solr Type of Cloud.")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor DEFAULT_COLLECTION = new PropertyDescriptor
            .Builder().name("Default Collection")
            .description("The Solr collection name, only used with a Solr Type of Cloud")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();


    private volatile SolrServer solrServer;

    @OnScheduled
    public final void onScheduled(final ProcessContext context) throws IOException {
        this.solrServer = createSolrServer(context);
        additionalOnScheduled(context);
    }

    /**
     * Create a SolrServer based on the type of Solr specified.
     *
     * @param context
     *          The context
     * @return an HttpSolrServer or CloudSolrServer
     */
    protected SolrServer createSolrServer(final ProcessContext context) {
        if (SOLR_TYPE_STANDARD.equals(context.getProperty(SOLR_TYPE).getValue())) {
            return new HttpSolrServer(context.getProperty(SOLR_LOCATION).getValue());
        } else {
            CloudSolrServer cloudSolrServer = new CloudSolrServer(
                    context.getProperty(SOLR_LOCATION).getValue());
            cloudSolrServer.setDefaultCollection(
                    context.getProperty(DEFAULT_COLLECTION).getValue());
            return cloudSolrServer;
        }
    }

    /**
     * Returns the {@link SolrServer} that was created by the
     * {@link #createSolrServer(org.apache.nifi.processor.ProcessContext)} method
     *
     * @return
     */
    protected final SolrServer getSolrServer() {
        return solrServer;
    }

    /**
     * Allows additional action to be taken during scheduling of processor.
     *
     * @param context
     *          The context
     */
    protected void additionalOnScheduled(final ProcessContext context) {

    }

    @Override
    protected final Collection<ValidationResult> customValidate(ValidationContext context) {
        final List<ValidationResult> problems = new ArrayList<>();

        if (SOLR_TYPE_CLOUD.equals(context.getProperty(SOLR_TYPE).getValue())) {
            final String collection = context.getProperty(DEFAULT_COLLECTION).getValue();
            if (collection == null || collection.trim().isEmpty()) {
                problems.add(new ValidationResult.Builder()
                        .subject(DEFAULT_COLLECTION.getName())
                        .input(collection).valid(false)
                        .explanation("A collection must specified for Solr Type of Cloud")
                        .build());
            }
        }

        Collection<ValidationResult> otherProblems = this.additionalCustomValidation(context);
        if (otherProblems != null) {
            problems.addAll(otherProblems);
        }

        return problems;
    }

    /**
     * Allows additional custom validation to be done. This will be called from
     * the parent's customValidation method.
     *
     * @param context
     *            The context
     * @return Validation results indicating problems
     */
    protected Collection<ValidationResult> additionalCustomValidation(ValidationContext context) {
        return new ArrayList<>();
    }

}
