package cz.vse.fcp.fcpapi.controller;


import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;

import uk.ac.manchester.cs.jfact.JFactFactory;

/**
 * Created by Ondřej Zamazal on 2016-2020.
 */

public class FCP {

    protected OWLOntology ontology;
    protected OWLOntologyManager manager;
    protected OWLDataFactory factory;
    Map<String, Object> data = new HashMap<>();

    //get categorization options for each ontology
    //first there is black list generation
    //then categorization options detection itself
    //onto is the URI of ontology, imports either true (including imports) or false (without imports)
    public Map<String, Object> getCategorizationOptions(String onto, boolean imports, List<String> selectedClasses) throws Exception {
        try {
            //init for each ontology
            this.manager = OWLManager.createOWLOntologyManager();
            this.manager.getOntologyConfigurator().setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
            this.manager.getOntologyConfigurator().setConnectionTimeout(500);
            this.ontology = this.manager.loadOntology(IRI.create(onto));
            this.factory = manager.getOWLDataFactory();


            //System.out.println(this.ontology.getClassesInSignature().size());

            //the black list
            //black list construction strategy
            //the combination (anchor class, property, thing)
            //this is created from the cases where the filler Thing
            //the combination (anchor class, property, filler)
            //this is created from the cases where the filler is the class or instance
            //there is always the combination (anchor class, property) added to the black list for pruning
            HashSet<String> ACpropertyFillerBlist = new HashSet<>();

            //19-12-19, creation class expressions corresponding to cases t2,t3,t4 (p1,p2,p3) for checking whether they are not already counted in t1 (p1) - kind of experiment implemention
            HashSet<OWLClassExpression> all_created_class_expressions;

            //use the reasoner
            this.factory = manager.getOWLDataFactory();
            //since 2019 I use JFact reasoner
            OWLReasonerFactory reasonerFactory = new JFactFactory();
            OWLReasonerConfiguration config = new SimpleConfiguration(5000);
            OWLReasoner reasoner = reasonerFactory.createReasoner(ontology, config);
            //System.out.println("to precompute inferences...");
            reasoner.precomputeInferences();

            //1. black list generation
            //iteration over all classes either with imports Imports.INCLUDED or without imports Imports.EXCLUDED
            Imports considerImport;
            if (imports)
                considerImport = Imports.INCLUDED;
            else
                considerImport = Imports.EXCLUDED;
            this.ontology.classesInSignature(considerImport).forEach((OWLClass cls) -> {
                //the iteration over all partial definitions and checking of different types of anonymous expressions
                EntitySearcher.getSuperClasses(cls, this.ontology).forEach((OWLClassExpression exp) -> {
                    //System.out.println("   "+cls+" rdfs:subClassOf:"+exp);
                    if (exp.isAnonymous()) {
                        if (exp instanceof OWLObjectSomeValuesFrom) {
                            String p = ((OWLObjectSomeValuesFrom) exp).getProperty().toString();

                            ACpropertyFillerBlist.add(cls.toString() + ";" + p + ";" + ((OWLObjectSomeValuesFrom) exp).getFiller());
                            ACpropertyFillerBlist.add(cls.toString() + ";" + p);

                            reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                    //the propagation to the subclasses
                                    if (!cls1.isBottomEntity()) {
                                        ACpropertyFillerBlist.add(cls1.toString() + ";" + p + ";" + ((OWLObjectSomeValuesFrom) exp).getFiller());
                                        ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                    }
                                }
                            });
                        }

                        if (exp instanceof OWLDataSomeValuesFrom) {
                            String p = ((OWLDataSomeValuesFrom) exp).getProperty().toString();
                            ACpropertyFillerBlist.add(cls.toString() + ";" + p);

                            reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                    //the propagation to the subclasses
                                    if (!cls1.isBottomEntity()) {
                                        ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                    }
                                }
                            });
                        }

                        if (exp instanceof OWLObjectHasValue) {
                            String p = ((OWLObjectHasValue) exp).getProperty().toString();
                            ACpropertyFillerBlist.add(cls.toString() + ";" + p + ";" + ((OWLObjectHasValue) exp).getFiller());
                            ACpropertyFillerBlist.add(cls.toString() + ";" + p);

                            reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                    //the propagation to the subclasses
                                    if (!cls1.isBottomEntity()) {
                                        ACpropertyFillerBlist.add(cls1.toString() + ";" + p + ";" + ((OWLObjectHasValue) exp).getFiller());
                                        ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                    }
                                }
                            });
                        }

                        if (exp instanceof OWLDataHasValue) {
                            String p = ((OWLDataHasValue) exp).getProperty().toString();
                            ACpropertyFillerBlist.add(cls.toString() + ";" + p);
                            reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                    //the propagation to the subclasses
                                    if (!cls1.isBottomEntity()) {
                                        ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                    }
                                }
                            });
                        }

                        if (exp instanceof OWLObjectHasSelf) {
                            String p = ((OWLObjectHasSelf) exp).getProperty().toString();
                            ACpropertyFillerBlist.add(cls.toString() + ";" + p);
                            reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                    //the propagation to the subclasses
                                    if (!cls1.isBottomEntity()) {
                                        ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                    }
                                }
                            });
                        }

                        if (exp instanceof OWLObjectMinCardinality) {
                            if (((OWLObjectMinCardinality) exp).getCardinality() >= 0) {
                                String p = ((OWLObjectMinCardinality) exp).getProperty().toString();
                                ACpropertyFillerBlist.add(cls.toString() + ";" + p + ";" + ((OWLObjectMinCardinality) exp).getFiller());
                                ACpropertyFillerBlist.add(cls.toString() + ";" + p);

                                reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                    for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                        //the propagation to the subclasses
                                        if (!cls1.isBottomEntity()) {
                                            ACpropertyFillerBlist.add(cls1.toString() + ";" + p + ";" + ((OWLObjectMinCardinality) exp).getFiller());
                                            ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                        }
                                    }
                                });
                            }
                        }

                        if (exp instanceof OWLDataMinCardinality) {
                            if (((OWLDataMinCardinality) exp).getCardinality() >= 0) {
                                String p = ((OWLDataMinCardinality) exp).getProperty().toString();
                                ACpropertyFillerBlist.add(cls.toString() + ";" + p);
                                reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                    for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                        //the propagation to the subclasses
                                        if (!cls1.isBottomEntity()) {
                                            ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                        }
                                    }
                                });
                            }
                        }

                        if (exp instanceof OWLObjectMaxCardinality) {
                            if (((OWLObjectMaxCardinality) exp).getCardinality() >= 0) {
                                String p = ((OWLObjectMaxCardinality) exp).getProperty().toString();
                                ACpropertyFillerBlist.add(cls.toString() + ";" + p + ";" + ((OWLObjectMaxCardinality) exp).getFiller());
                                ACpropertyFillerBlist.add(cls.toString() + ";" + p);

                                reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                    for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                        //the propagation to the subclasses
                                        if (!cls1.isBottomEntity()) {
                                            ACpropertyFillerBlist.add(cls1.toString() + ";" + p + ";" + ((OWLObjectMaxCardinality) exp).getFiller());
                                            ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                        }
                                    }
                                });
                            }
                        }

                        if (exp instanceof OWLDataMaxCardinality) {
                            if (((OWLDataMaxCardinality) exp).getCardinality() >= 0) {
                                String p = ((OWLDataMaxCardinality) exp).getProperty().toString();
                                ACpropertyFillerBlist.add(cls.toString() + ";" + p);
                                reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                    for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                        //the propagation to the subclasses
                                        if (!cls1.isBottomEntity()) {
                                            ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                        }
                                    }
                                });
                            }
                        }

                        if (exp instanceof OWLObjectExactCardinality) {
                            if (((OWLObjectExactCardinality) exp).getCardinality() >= 0) {
                                String p = ((OWLObjectExactCardinality) exp).getProperty().toString();
                                ACpropertyFillerBlist.add(cls.toString() + ";" + p + ";" + ((OWLObjectExactCardinality) exp).getFiller());
                                ACpropertyFillerBlist.add(cls.toString() + ";" + p);

                                reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                    for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                        //the propagation to the subclasses
                                        if (!cls1.isBottomEntity()) {
                                            ACpropertyFillerBlist.add(cls1.toString() + ";" + p + ";" + ((OWLObjectExactCardinality) exp).getFiller());
                                            ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                        }
                                    }
                                });
                            }
                        }

                        if (exp instanceof OWLDataExactCardinality) {
                            if (((OWLDataExactCardinality) exp).getCardinality() >= 0) {
                                String p = ((OWLDataExactCardinality) exp).getProperty().toString();
                                ACpropertyFillerBlist.add(cls.toString() + ";" + p);
                                reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                    for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                        //the propagation to the subclasses
                                        if (!cls1.isBottomEntity()) {
                                            ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                        }
                                    }
                                });
                            }
                        }
                    }
                });
                //the iteration over all complete definitions
//                for(OWLClassExpression exp : EntitySearcher.getEquivalentClasses(cls, this.ontology)) {
                EntitySearcher.getEquivalentClasses(cls, this.ontology).forEach((OWLClassExpression exp) -> {
                    if (exp.isAnonymous()) {

                        if (exp instanceof OWLObjectSomeValuesFrom) {
                            String p = ((OWLObjectSomeValuesFrom) exp).getProperty().toString();
                            ACpropertyFillerBlist.add(cls.toString() + ";" + p + ";" + ((OWLObjectSomeValuesFrom) exp).getFiller());
                            ACpropertyFillerBlist.add(cls.toString() + ";" + p);

                            reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                    //the propagation to the subclasses
                                    if (!cls1.isBottomEntity()) {
                                        ACpropertyFillerBlist.add(cls1.toString() + ";" + p + ";" + ((OWLObjectSomeValuesFrom) exp).getFiller());
                                        ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                    }
                                }
                            });
                        }

                        if (exp instanceof OWLDataSomeValuesFrom) {
                            String p = ((OWLDataSomeValuesFrom) exp).getProperty().toString();
                            ACpropertyFillerBlist.add(cls.toString() + ";" + p);
                            reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                    //the propagation to the subclasses
                                    if (!cls1.isBottomEntity()) {
                                        ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                    }
                                }
                            });
                        }

                        if (exp instanceof OWLObjectHasValue) {
                            String p = ((OWLObjectHasValue) exp).getProperty().toString();
                            ACpropertyFillerBlist.add(cls.toString() + ";" + p + ";" + ((OWLObjectHasValue) exp).getFiller());
                            ACpropertyFillerBlist.add(cls.toString() + ";" + p);

                            reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                    //the propagation to the subclasses
                                    if (!cls1.isBottomEntity()) {
                                        ACpropertyFillerBlist.add(cls1.toString() + ";" + p + ";" + ((OWLObjectHasValue) exp).getFiller());
                                        ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                    }
                                }
                            });
                        }

                        if (exp instanceof OWLDataHasValue) {
                            String p = ((OWLDataHasValue) exp).getProperty().toString();
                            ACpropertyFillerBlist.add(cls.toString() + ";" + p);
                            reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                    //the propagation to the subclasses
                                    if (!cls1.isBottomEntity()) {
                                        ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                    }
                                }
                            });
                        }

                        if (exp instanceof OWLObjectHasSelf) {
                            String p = ((OWLObjectHasSelf) exp).getProperty().toString();
                            ACpropertyFillerBlist.add(cls.toString() + ";" + p);
                            reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                    //the propagation to the subclasses
                                    if (!cls1.isBottomEntity()) {
                                        ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                    }
                                }
                            });
                        }

                        if (exp instanceof OWLObjectMinCardinality) {
                            if (((OWLObjectMinCardinality) exp).getCardinality() >= 0) {
                                String p = ((OWLObjectMinCardinality) exp).getProperty().toString();
                                ACpropertyFillerBlist.add(cls.toString() + ";" + p + ";" + ((OWLObjectMinCardinality) exp).getFiller());
                                ACpropertyFillerBlist.add(cls.toString() + ";" + p);

                                reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                    for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                        //the propagation to the subclasses
                                        if (!cls1.isBottomEntity()) {
                                            ACpropertyFillerBlist.add(cls1.toString() + ";" + p + ";" + ((OWLObjectMinCardinality) exp).getFiller());
                                            ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                        }
                                    }
                                });
                            }
                        }

                        if (exp instanceof OWLDataMinCardinality) {
                            if (((OWLDataMinCardinality) exp).getCardinality() >= 0) {
                                String p = ((OWLDataMinCardinality) exp).getProperty().toString();
                                ACpropertyFillerBlist.add(cls.toString() + ";" + p);
                                reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                    for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                        //the propagation to the subclasses
                                        if (!cls1.isBottomEntity()) {
                                            ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                        }
                                    }
                                });
                            }
                        }

                        if (exp instanceof OWLObjectMaxCardinality) {
                            if (((OWLObjectMaxCardinality) exp).getCardinality() >= 0) {
                                String p = ((OWLObjectMaxCardinality) exp).getProperty().toString();
                                ACpropertyFillerBlist.add(cls.toString() + ";" + p + ";" + ((OWLObjectMaxCardinality) exp).getFiller());
                                ACpropertyFillerBlist.add(cls.toString() + ";" + p);

                                reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                    for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                        //the propagation to the subclasses
                                        if (!cls1.isBottomEntity()) {
                                            ACpropertyFillerBlist.add(cls1.toString() + ";" + p + ";" + ((OWLObjectMaxCardinality) exp).getFiller());
                                            ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                        }
                                    }
                                });
                            }
                        }

                        if (exp instanceof OWLDataMaxCardinality) {
                            if (((OWLDataMaxCardinality) exp).getCardinality() >= 0) {
                                String p = ((OWLDataMaxCardinality) exp).getProperty().toString();
                                ACpropertyFillerBlist.add(cls.toString() + ";" + p);
                                reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                    for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                        //the propagation to the subclasses
                                        if (!cls1.isBottomEntity()) {
                                            ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                        }
                                    }
                                });
                            }
                        }

                        if (exp instanceof OWLObjectExactCardinality) {
                            if (((OWLObjectExactCardinality) exp).getCardinality() >= 0) {
                                String p = ((OWLObjectExactCardinality) exp).getProperty().toString();
                                ACpropertyFillerBlist.add(cls.toString() + ";" + p + ";" + ((OWLObjectExactCardinality) exp).getFiller());
                                ACpropertyFillerBlist.add(cls.toString() + ";" + p);

                                reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                    for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                        //the propagation to the subclasses
                                        if (!cls1.isBottomEntity()) {
                                            ACpropertyFillerBlist.add(cls1.toString() + ";" + p + ";" + ((OWLObjectExactCardinality) exp).getFiller());
                                            ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                        }
                                    }
                                });
                            }
                        }

                        if (exp instanceof OWLDataExactCardinality) {
                            if (((OWLDataExactCardinality) exp).getCardinality() >= 0) {
                                String p = ((OWLDataExactCardinality) exp).getProperty().toString();
                                ACpropertyFillerBlist.add(cls.toString() + ";" + p);
                                reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                                    for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                                        //the propagation to the subclasses
                                        if (!cls1.isBottomEntity()) {
                                            ACpropertyFillerBlist.add(cls1.toString() + ";" + p);
                                        }
                                    }
                                });
                            }
                        }
                    }
                });
            });
            String writePath = System.getProperty("catalina.base") + "/temp/";
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                writePath = "";
            }
            //black list generation end
            System.out.println("blist:");
            System.out.println(ACpropertyFillerBlist);
            //control output to the text file
            PrintWriter toFileBL = new PrintWriter(new FileWriter(writePath + "blackList", true));
            toFileBL.println(onto);
            for (String s : ACpropertyFillerBlist) {
                toFileBL.println(s);
            }
            toFileBL.println();
            toFileBL.close();

            //2. categorization options detection itself - variants t1, t2, t3, t4 (t5 is skipped for now)
            PrintWriter toFile = new PrintWriter(new FileWriter(writePath + "a.txt", true));
            toFile.println(onto);

            //19-12-19
            //all_created_class_expressions - work in progress
            all_created_class_expressions = new HashSet<>();

            //fist go for variants t2, t3, t4
            List<String> list2 = new ArrayList<>();
            List<String> list3 = new ArrayList<>();
            List<String> list4 = new ArrayList<>();
            this.ontology.objectPropertiesInSignature(considerImport).forEach((OWLObjectProperty op) -> {
                Iterator<Node<OWLClass>> it = reasoner.getObjectPropertyDomains(op, false).nodes().iterator();
                while (it.hasNext()) {
                    Node<OWLClass> n = it.next();
                    //it does not consider Nothing
                    if (n.isBottomNode()) break;

                    for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                        if (selectedClasses.size() > 0 && !selectedClasses.contains(cls1.getIRI().toString())) {
                            continue;
                        }
                        //t2
                        if (ACpropertyFillerBlist.contains(cls1.toString() + ";" + op.toString())) {
                            System.out.println("blist applied:" + cls1.toString() + ";" + op.toString());
                            continue; //pruning
                        }
                        System.out.println("$t2 | exists | " + op + ".owl:Thing | rdfs:subClassOf | " + cls1);
                        list2.add("$t2 | exists | " + op + ".owl:Thing | rdfs:subClassOf | " + cls1);
                        toFile.println("$t2 | exists | " + op + ".owl:Thing | rdfs:subClassOf | " + cls1);

                        //19-12-19, creation corresponding class expression
                        OWLClassExpression clsExpr1 = this.factory.getOWLObjectSomeValuesFrom(op, factory.getOWLThing());
                        all_created_class_expressions.add(clsExpr1);
                        OWLClassExpression clsExpr3 = this.factory.getOWLObjectSomeValuesFrom(op, factory.getOWLThing());
                        OWLClassExpression clsExpr4 = factory.getOWLObjectIntersectionOf(cls1, clsExpr3);
                        if (!reasoner.isSatisfiable(clsExpr4)) {
                            System.err.println(reasoner.isConsistent());
                            System.err.println(clsExpr4);
                            System.err.println("Found CE which is not satisfiable ($t2)");
//                            System.exit(1);
//                            continue;
                        }

                        System.out.println();

                        //start t3
                        HashSet<String> subclasses = new HashSet<>();
                        HashSet<String> individuals = new HashSet<>();
                        EntitySearcher.getRanges(op, this.ontology).forEach((OWLClassExpression exp) -> {
                            if (exp instanceof OWLClass) {
                                OWLClass cls2 = (OWLClass) exp;
                                reasoner.getSubClasses(cls2, false).nodes().forEach((Node<OWLClass> n2) -> {
                                    for (OWLClass cls3 : n2.getEntitiesMinusBottom()) {
                                        if (ACpropertyFillerBlist.contains(cls1.toString() + ";" + op.toString() + ";" + cls3.toString())) {
                                            System.out.println("blist applied:" + cls1.toString() + ";" + op.toString() + ";" + cls3.toString());
                                            continue; //pruning
                                        }
                                        subclasses.add(cls3.toString());
                                    }
                                });
                                //start t4
                                reasoner.getInstances(cls2, false).nodes().forEach((Node<OWLNamedIndividual> n2) -> n2.entities().filter((OWLNamedIndividual ind) -> {
                                    if (ACpropertyFillerBlist.contains(cls1.toString() + ";" + op.toString() + ";" + ind.toString())) {
                                        System.out.println("blist applied:" + cls1.toString() + ";" + op.toString() + ";" + ind.toString());
                                        return false; //pruning
                                    }
                                    return true;
                                }).forEach((OWLNamedIndividual ind) -> individuals.add(ind.toString())));
                                //end t4
                            }
                        });
                        if (!subclasses.isEmpty()) {
                            //System.out.println("$t3 "+subclasses+" classification option (via op "+op+") for anchor class:"+cls1);;
                            for (String x : subclasses) {
                                System.out.println("$t3 | exists | " + op + "." + x + " | rdfs:subClassOf | " + cls1);

                                list3.add("$t3 | exists | " + op + "." + x + " | rdfs:subClassOf | " + cls1);
                                toFile.println("$t3 | exists | " + op + "." + x + " | rdfs:subClassOf | " + cls1);
                                toFile.println("");
                                System.out.println();
                                //19-12-19, start, corresponding class expression
                                clsExpr1 = this.factory.getOWLObjectSomeValuesFrom(op, this.factory.getOWLClass(IRI.create(x.replaceAll("<", "").replaceAll(">", ""))));
                                all_created_class_expressions.add(clsExpr1);
                                clsExpr3 = this.factory.getOWLObjectSomeValuesFrom(op, this.factory.getOWLClass(IRI.create(x.replaceAll("<", "").replaceAll(">", ""))));
                                clsExpr4 = factory.getOWLObjectIntersectionOf(cls1, clsExpr3);
                                if (!reasoner.isSatisfiable(clsExpr4)) {
                                    System.err.println(reasoner.isConsistent());
                                    System.err.println(clsExpr4);
                                    System.err.println("Found CE which is not satisfiable ($t3)");
//                                    System.exit(1);
//                                    continue;
                                }
                            }

                        }
                        if (!individuals.isEmpty())
                            for (String x : individuals) {
                                System.out.println("$t4 | exists | " + op + ".{" + x + "} | rdfs:subClassOf | " + cls1);
                                System.out.println();

                                list4.add("$t4 | exists | " + op + ".{" + x + "} | rdfs:subClassOf | " + cls1);
                                toFile.println("$t4 | exists | " + op + ".{" + x + "} | rdfs:subClassOf | " + cls1);
                                toFile.println("");
                                //19-12-19, start, corresponding class expression
                                clsExpr1 = this.factory.getOWLObjectSomeValuesFrom(op, (this.factory.getOWLObjectOneOf(this.factory.getOWLNamedIndividual(IRI.create(x.replaceAll("<", "").replaceAll(">", ""))))));
                                all_created_class_expressions.add(clsExpr1);
                                clsExpr3 = this.factory.getOWLObjectSomeValuesFrom(op, (this.factory.getOWLObjectOneOf(this.factory.getOWLNamedIndividual(IRI.create(x.replaceAll("<", "").replaceAll(">", ""))))));
                                clsExpr4 = factory.getOWLObjectIntersectionOf(cls1, clsExpr3);
                                if (!reasoner.isSatisfiable(clsExpr4)) {
                                    System.err.println(reasoner.isConsistent());
                                    System.err.println(clsExpr4);
                                    System.err.println("Found CE which is not satisfiable ($t4)");
//                                    System.exit(1);
//                                    continue;
                                }
                            }
                        //end t3
                    }
                }
            });
            data.put("t2", list2);
            data.put("t3", list3);
            data.put("t4", list4);

            //for dp compute t2


            this.ontology.dataPropertiesInSignature(considerImport).forEach((OWLDataProperty dp) -> {
                Iterator<Node<OWLClass>> it = reasoner.getDataPropertyDomains(dp, false).nodes().iterator();
                while (it.hasNext()) {
                    Node<OWLClass> n = it.next();
                    //if domain is Nothing not considering this
                    if (n.isBottomNode()) break;
                    for (OWLClass cls1 : n.getEntitiesMinusTop()) {
                        if (selectedClasses.size() > 0 && !selectedClasses.contains(cls1.getIRI().toString())) {
                            continue;
                        }
                        if (ACpropertyFillerBlist.contains(cls1.toString() + ";" + dp.toString())) {
                            System.out.println("blist applied:" + cls1.toString() + ";" + dp.toString());
                            continue; //pruning
                        }
                        System.out.println("$t2 | (dp) exists | " + dp + ".owl:Thing | rdfs:subClassOf | " + cls1);
                        list2.add("$t2 | (dp) exists | " + dp + ".owl:Thing | rdfs:subClassOf | " + cls1);
                        System.out.println();
                        toFile.println("$t2 | (dp) exists | " + dp + ".owl:Thing | rdfs:subClassOf | " + cls1);
                        toFile.println("");
                        //19-12-19, start, corresponding odpovidajici class expression for dp owl:Thing not possible
                        //end dp t2
                    }
                }
            });
            data.put("t2", list2);
            //end categorization options detection itself t2, t3, t4 but t1 is in the following iteration

            //3. compute t1
            List<String> list1 = new ArrayList<>();
            this.ontology.classesInSignature(considerImport).filter((OWLClass cls) -> {
                if (selectedClasses.size() == 0) {
                    return true;
                } else {
                    return selectedClasses.contains(cls.getIRI().toString());
                }
            }).filter((OWLClass cls) -> !cls.isTopEntity()).forEach((OWLClass cls) -> {
                //compute and print t1
                HashSet<String> subclasses = new HashSet<>();
                HashSet<OWLClass> subclasses1 = new HashSet<>();

                reasoner.getSubClasses(cls, false).nodes().forEach((Node<OWLClass> n) -> {
                    if (n.getEntitiesMinusBottom().size() > 0) {
                        subclasses.add(n.getEntitiesMinusBottom().toString());
                        //19-12-19, because of finding CE already as equivalence for subclasses)
                        subclasses1.addAll(n.getEntitiesMinusBottom());
                        //19-12-19, end
                    }
                });
                if (subclasses.size() > 0) {
                    System.out.println("$t1 | " + cls + " | " + subclasses);
                    list1.add("$t1 | " + cls + " | " + subclasses);
                    toFile.println("$t1 | " + cls + " | " + subclasses);
                    //19-12-19, checking whether named subclass  is not the same as cases from t2, t3, t4
                    //if so then it is output but not counted
                    //this part is in progress
                    System.out.println(subclasses1);
                    //toFile.println(subclasses1);
                    //the following work in progress
                    for (OWLClassExpression owlExpr1 : all_created_class_expressions) {
                        if (!reasoner.getEquivalentClasses(owlExpr1).entities().findAny().isPresent()) {
                            reasoner.getEquivalentClasses(owlExpr1).entities().forEach(System.out::println);
                        }
                    }
                    //19-12-19, end
                }

                toFile.println();
            });

            data.put("t1", list1);
            toFile.close();

            System.out.println("done");
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
        return data;
    }


    //ArrayList<String> ontologies = new ArrayList<String>();

}
