/**
 * 
 */
package org.unicode.cldr.web;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.test.SimpleTestCache;
import org.unicode.cldr.test.TestCache;
import org.unicode.cldr.test.TestCache.TestResultBundle;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRLocale.SublocaleProvider;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.VoteResolver.Status;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.XPathParts.Comments;
import org.unicode.cldr.web.STFactory.DataBackedSource;
import org.unicode.cldr.web.UserRegistry.User;

import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.dev.test.util.ElapsedTimer;

/**
 * @author srl
 *
 */
public class STFactory extends Factory implements BallotBoxFactory<UserRegistry.User>, SublocaleProvider, UserRegistry.UserChangedListener {
	/**
	 * If true: run EVERY xpath through the resolver.
	 */
    public static final boolean RESOLVE_ALL_XPATHS = false;

	public class DataBackedSource extends DelegateXMLSource {
        PerLocaleData ballotBox;
        XMLSource aliasOf; // original XMLSource
        public DataBackedSource(PerLocaleData makeFrom) {
            super( (XMLSource)makeFrom.diskData.cloneAsThawed());
            ballotBox = makeFrom;
        }


        /* (non-Javadoc)
         * @see com.ibm.icu.util.Freezable#freeze()
         */
        @Override
        public Object freeze() {
            readonly();
            return null;
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#getFullPathAtDPath(java.lang.String)
         */
        @Override
        public String getFullPathAtDPath(String path) {
            //			Map<User,String> m = ballotBox.peekXpathToVotes(path);
            //			if(m==null || m.isEmpty()) {
            //				return aliasOf.getFullPathAtDPath(path);
            //			} else {
            //				SurveyLog.logger.warning("Note: DBS.getFullPathAtDPath() todo!!"); TODO: show losing values
            return delegate.getFullPathAtDPath(path);
            //			}
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#getValueAtDPath(java.lang.String)
         */
        @Override
        public String getValueAtDPath(String path) {
            return delegate.getValueAtDPath(path);
        }

        //		/**
        //		 * @param path
        //		 * @return
        //		 */
        //		private String valueFromResolver(String path, VoteResolver<String> resolver) {
        //			Map<User,String> m = ballotBox.peekXpathToVotes(path);
        //			if(m==null || m.isEmpty()) {
        //                String res =  delegate.getValueAtDPath(path);
        //			    return res;
        //			} else {
        //				String res = ballotBox.getResolver(m,path, resolver).getWinningValue();
        //                return res;
        //			}
        //		}
        //		
        public VoteResolver<String> setValueFromResolver(String path, VoteResolver<String> resolver) {
            Map<User,String> m = ballotBox.peekXpathToVotes(path);
            String res;
            if((m==null || m.isEmpty()) && !RESOLVE_ALL_XPATHS) { // no votes, so..
                res = ballotBox.diskData.getValueAtDPath(path);
            } else {
                res = (resolver=ballotBox.getResolver(m,path, resolver)).getWinningValue();
            }
            //			SurveyLog.logger.info(path+"="+res+", by resolver.");
            if(res!=null) {
                delegate.putValueAtDPath(path, res);
            } else {
                delegate.removeValueAtDPath(path);
            }
            notifyListeners(path);
            return resolver;
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#getXpathComments()
         */
        @Override
        public Comments getXpathComments() {
            return delegate.getXpathComments();
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#iterator()
         */
        @Override
        public Iterator<String> iterator() {
            if(ballotBox.xpathToVotes == null || ballotBox.xpathToVotes.isEmpty()) {
                return delegate.iterator();
            } else {
                SurveyLog.debug("Note: DBS.iterator() todo -- iterate over losing values?");
                return delegate.iterator();
            }
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#putFullPathAtDPath(java.lang.String, java.lang.String)
         */
        @Override
        public void putFullPathAtDPath(String distinguishingXPath,
                String fullxpath) {
            readonly();
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#putValueAtDPath(java.lang.String, java.lang.String)
         */
        @Override
        public void putValueAtDPath(String distinguishingXPath, String value) {
            readonly();
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#removeValueAtDPath(java.lang.String)
         */
        @Override
        public void removeValueAtDPath(String distinguishingXPath) {
            readonly();
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#setXpathComments(org.unicode.cldr.util.XPathParts.Comments)
         */
        @Override
        public void setXpathComments(Comments comments) {
            readonly();
        }


        //		/* (non-Javadoc)
        //		 * @see org.unicode.cldr.util.XMLSource#make(java.lang.String)
        //		 */
        //		@Override
        //		public XMLSource make(String localeID) {
        //			return makeSource(localeID, this.isResolving());
        //		}

        //		/* (non-Javadoc)
        //		 * @see org.unicode.cldr.util.XMLSource#getAvailableLocales()
        //		 */
        //		@SuppressWarnings("rawtypes")
        //		@Override
        //		public Set getAvailableLocales() {
        //			return handleGetAvailable();
        //		}

        //		/* (non-Javadoc)
        //		 * @see org.unicode.cldr.util.XMLSource#getSupplementalDirectory()
        //		 */
        //		@Override
        //		public File getSupplementalDirectory() {
        //			File suppDir =  new File(getSourceDirectory()+"/../"+"supplemental");
        //			return suppDir;
        //		}


        //		@Override
        //		protected synchronized TreeMap<String, String> getAliases() {
        //			if(true) throw new InternalError("NOT IMPLEMENTED.");
        //			return null;
        //		}

    }

    /**
     * the STFactory maintains exactly one instance of this class per locale it is working with. It contains the XMLSource, Example Generator, etc..
     * @author srl
     *
     */
    private class PerLocaleData implements Comparable<PerLocaleData>, BallotBox<User>  {
        private CLDRFile file = null, rFile = null;
        private CLDRLocale locale;
        private CLDRFile oldFile;
        private boolean readonly;
        private MutableStamp stamp = MutableStamp.getInstance();

        /**
         * The held XMLSource.
         */
        private DataBackedSource xmlsource = null;
        /**
         * The on-disk data. May be == to xmlsource for readonly data.
         */
        private XMLSource diskData = null;
        private CLDRFile diskFile = null;

        /* SIMPLE IMP */
        private Map<String, Map<User,String>> xpathToVotes = new HashMap<String,Map<User,String>>();
        private Set <User> allVoters = new TreeSet<User>();
        private boolean oldFileMissing;
        private XMLSource resolvedXmlsource = null;


        PerLocaleData(CLDRLocale locale) {
            this.locale = locale;
            readonly = isReadOnlyLocale(locale);
            diskData=(XMLSource)sm.getDiskFactory().makeSource(locale.getBaseName()).freeze();
            sm.xpt.loadXPaths(diskData);
            diskFile = sm.getDiskFactory().make(locale.getBaseName(), true).freeze();
        }

        public final Stamp getStamp() {
            return stamp;
        }
        /**
         * Load internal data , push into source.
         * @param dataBackedSource 
         * @return 
         */
        private DataBackedSource loadVoteValues(DataBackedSource dataBackedSource)  {
            if(!readonly) {
                VoteResolver<String> resolver= null; //save recalculating this.
                Set<String> hitXpaths = new HashSet<String>();
                ElapsedTimer et = (SurveyLog.DEBUG)?new ElapsedTimer("Loading PLD for " + locale):null;
                Connection conn = null;
                PreparedStatement ps = null;
                ResultSet rs = null;
                int n = 0;
                try {
                    conn = DBUtils.getInstance().getDBConnection();
                    ps = openQueryByLocale(conn);
                    ps.setString(1, locale.getBaseName());
                    rs = ps.executeQuery();

                    while(rs.next()) {
                        int xp = rs.getInt(1);
                        String xpath = sm.xpt.getById(xp);
                        hitXpaths.add(xpath);
                        int submitter = rs.getInt(2);
                        String value = DBUtils.getStringUTF8(rs, 3);
                        internalSetVoteForValue(sm.reg.getInfo(submitter), xpath, value, resolver, dataBackedSource);
                        n++;
                    }

                } catch (SQLException e) {
                    SurveyLog.logException(e);
                    SurveyMain.busted("Could not read locale " + locale, e);
                    throw new InternalError("Could not load locale " + locale + " : " + DBUtils.unchainSqlException(e));
                } finally {
                    DBUtils.close(rs,ps,conn);
                }
                SurveyLog.debug(et + " - read " + n + " items.");
                if(RESOLVE_ALL_XPATHS) {
                	et = (SurveyLog.DEBUG)?new ElapsedTimer("Loading PLD for " + locale):null;
                	int j=0;
                	for(String xp : diskData) {
                		if(hitXpaths.contains(xp))
                			continue;
                        resolver=dataBackedSource.setValueFromResolver(xp, resolver);
                		j++;
                	}
                    SurveyLog.debug(et + " - RESOLVE_ALL_XPATHS  - resolved " + j + " additional items, " + n + " total.");
                }
            }
            stamp.next();
            dataBackedSource.addListener(gTestCache);
            return dataBackedSource;
        }

        @Override
        public int compareTo(PerLocaleData arg0) {
            if(this==arg0) {
                return 0;
            } else {
                return locale.compareTo(arg0.locale);
            }
        }

        @Override
        public boolean equals(Object other) {
            if(other==this) {
                return true;
            } else if(!(other instanceof PerLocaleData)) {
                return false;
            } else {
                return ((PerLocaleData)other).locale.equals(locale);
            }
        }

        public synchronized CLDRFile getFile(boolean resolved) {
            if(resolved) {
            	if(true) { // reuse r-files?
            		return new CLDRFile(makeSource(true)).setSupplementalDirectory(getSupplementalDirectory());
            	} else {
	                if(rFile == null) {
	                    if(getSupplementalDirectory()==null) throw new InternalError("getSupplementalDirectory() == null!");
	                    rFile = new CLDRFile(makeSource(true)).setSupplementalDirectory(getSupplementalDirectory());
	                    rFile.getSupplementalDirectory();
	                }
	                return rFile;
            	}
            } else {
                if(file == null) {
                    if(getSupplementalDirectory()==null) throw new InternalError("getSupplementalDirectory() == null!");
                    file = new CLDRFile(makeSource(false)).setSupplementalDirectory(getSupplementalDirectory());
                }
                return file;
            }
        }

        public synchronized CLDRFile getOldFile() {
            if(oldFile==null && !oldFileMissing) {
                oldFileMissing = !sm.getOldFactory().getAvailable().contains(locale.getBaseName());
                if(!oldFileMissing) {
                    oldFile = sm.getOldFactory().make(locale.getBaseName(), true);
                }
            }
            return oldFile;
        }

        //		public VoteResolver<String> getResolver(Map<User, String> m, String path) {
        //			return getResolver(m, path, null);
        //		}

        
        private VoteResolver<String> getResolverInternal(Map<User,String> m, String path, VoteResolver<String> r) {
            //			if(m==null) throw new InternalError("no Map for " + path);
            if(path==null) throw new IllegalArgumentException("path must not be null");
            if(r==null) {
                r = new VoteResolver<String>();
            } else {
                r.clear();
            }
            // Set established locale
            r.setEstablishedFromLocale(diskFile.getLocaleID());
            XPathParts xpp = new XPathParts(null,null);
            CLDRFile anOldFile = getOldFile();
            if(anOldFile==null) anOldFile = diskFile;
            String fullXPath = anOldFile.getFullXPath(path);
            if(fullXPath==null) fullXPath = path; // throw new InternalError("null full xpath for " + path);
            xpp.set(fullXPath);
            final String lastValue = anOldFile.getStringValue(path);
            final String srcid = anOldFile.getSourceLocaleID(path, null);
            String draft = xpp.getAttributeValue(-1, LDMLConstants.DRAFT);
            
            Status lastStatus = draft==null?Status.approved:
                VoteResolver.Status.fromString(draft);
            if ( !srcid.equals(diskFile.getLocaleID())) {
            	lastStatus = Status.missing;
            }
            

            if(false) System.err.println(fullXPath + " : " + xpp.getAttributeValue(-1, LDMLConstants.DRAFT) + " == " + lastStatus + " ('"+lastValue+"')");

            r.setLastRelease(lastValue, lastValue==null?Status.missing:lastStatus);
            String currentValue = diskData.getValueAtDPath(path);
            r.add(currentValue); /* add the current value. */
            //			SurveyLog.logger.warning(path + ": LR '"+lastValue+"', " + lastStatus);
            if(m!=null) {
                for(Map.Entry<User, String>e : m.entrySet()) {
                    r.add(e.getValue(), e.getKey().id);
                    //if(true)			SurveyLog.logger.warning(path + ": added  '"+e.getValue()+"', for  " + e.getKey().toString());
                }
            } else {
                //				SurveyLog.logger.warning("m is null for " + path + " , but last release value is " + getOldFile().getStringValue(path));
            }
            //			SurveyLog.logger.warning("RESOLVER for " + path + " --> " + r.toString());
            return r;
        }
        
        public VoteResolver<String> getResolver(Map<User, String> m, String path, VoteResolver<String> r) {
        	try  {
        		r = getResolverInternal(m,path,r);
        	} catch (VoteResolver.UnknownVoterException uve) {
        		handleUserChanged(null);
        		try {
        			r = getResolverInternal(m, path, r);
        		} catch(VoteResolver.UnknownVoterException uve2) {
        			SurveyLog.logException(uve2);
        			sm.busted(uve2.toString(), uve2);
        			throw new InternalError(uve2.toString());
        		}
        	}
        	return r;
        }

        @Override
        public VoteResolver<String> getResolver(String path) {
            return getResolver(peekXpathToVotes(path),path, null);
        }

        @Override
        public Set<String> getValues(String xpath) {
            Map<User,String> m = peekXpathToVotes(xpath);
            if(m==null) {
                return null;
            } else {
                Set<String> ts = new TreeSet<String>();
                ts.addAll(m.values());

                // include the on-disk value, if not present.
                String fbValue = diskData.getValueAtDPath(xpath);
                if(fbValue!=null) {
                    ts.add(fbValue);
                }
                return ts;
            }
        }

        @Override
        public Set<User> getVotesForValue(String xpath, String value) {
            Map<User,String> m = peekXpathToVotes(xpath);
            if(m==null) {
                return null;
            } else {
                TreeSet<User> ts = new TreeSet<User>();
                for(Map.Entry<User,String> e : m.entrySet()) {
                    if(e.getValue().equals(value)) {
                        ts.add(e.getKey());
                    }
                }
                if(ts.isEmpty()) return null;
                return ts;
            }
        }

        @Override
        public String getVoteValue(User user, String distinguishingXpath) {
            Map<User,String> m = peekXpathToVotes(distinguishingXpath);
            if(m!=null) {
                return m.get(user);
            } else {
                return null;
            }
        }
        /**
         * x->v map, create if not there
         * @param xpath
         * @return
         */
        private synchronized final Map<User,String> getXpathToVotes(String xpath) {
            Map<User,String> m = peekXpathToVotes(xpath);
            if(m==null) {
                m = new TreeMap<User,String>(); // use a treemap, don't expect it to be large enough to need a hash
                xpathToVotes.put(xpath, m);
            }
            return m;
        }

        public synchronized XMLSource makeSource(boolean resolved) {
            if(resolved==true) {
            	if(true) { // cache r-sources?
            		return makeResolvingSource(locale.getBaseName(), getMinimalDraftStatus());
            	} else {
	            	if(resolvedXmlsource==null) {
	            		resolvedXmlsource = makeResolvingSource(locale.getBaseName(), getMinimalDraftStatus());
	            	}
	            	return resolvedXmlsource ;
            	}
            } else {
                if(readonly) {
				    return diskData;
				} else {
				    if(xmlsource == null) {
				        xmlsource = loadVoteValues(new DataBackedSource(this));
				    }
				    return xmlsource;
				}
            }
        }

        /**
         * get x->v map, DONT create it if not there
         * @param xpath
         * @return
         */
        private final Map<User,String> peekXpathToVotes(String xpath) {
            return xpathToVotes.get(xpath);
        }


        @Override
        public synchronized void voteForValue(User user, String distinguishingXpath,
                String value) {
            SurveyLog.debug("V4v: "+locale+" "+distinguishingXpath + " : " + user + " voting for '" + value + "'");

            if(!readonly) {
                makeSource(false);
                ElapsedTimer et = !SurveyLog.DEBUG?null:new ElapsedTimer("Recording PLD for " + locale+" "+distinguishingXpath + " : " + user + " voting for '" + value);
                Connection conn = null;
                PreparedStatement ps = null;
                PreparedStatement ps2 = null;
                ResultSet rs = null;
                int xpathId = sm.xpt.getByXpath(distinguishingXpath);
                int submitter = user.id;
                try {
                    conn = DBUtils.getInstance().getDBConnection();
                    if(DBUtils.db_Mysql) { //  use 'on duplicate key' syntax 
                        ps = DBUtils.prepareForwardReadOnly(conn,"INSERT INTO " + CLDR_VBV + " (locale,xpath,submitter,value) values (?,?,?,?) " + 
                                "ON DUPLICATE KEY UPDATE locale=?,xpath=?,submitter=?,value=?");

                        ps.setString(5, locale.getBaseName());
                        ps.setInt(6, xpathId);
                        ps.setInt(7,submitter);
                        DBUtils.setStringUTF8(ps, 8, value);
                    } else {
                        ps2 =  DBUtils.prepareForwardReadOnly(conn, "DELETE FROM " + CLDR_VBV + " where locale=? and xpath=? and submitter=? ");
                        ps =  DBUtils.prepareForwardReadOnly(conn, "INSERT INTO  " + CLDR_VBV + " (locale,xpath,submitter,value) VALUES (?,?,?,?) ");

                        ps2.setString(1, locale.getBaseName());
                        ps2.setInt(2, xpathId);
                        ps2.setInt(3,submitter);
                    }

                    ps.setString(1, locale.getBaseName());
                    ps.setInt(2,xpathId);
                    ps.setInt(3,submitter);
                    DBUtils.setStringUTF8(ps, 4, value);

                    if(!DBUtils.db_Mysql) {
                        ps2.executeUpdate();
                    }
                    ps.executeUpdate();

                    conn.commit();
                } catch (SQLException e) {
                    SurveyLog.logException(e);
                    SurveyMain.busted("Could not read locale " + locale, e);
                    throw new InternalError("Could not load locale " + locale + " : " + DBUtils.unchainSqlException(e));
                } finally {
                    DBUtils.close(rs,ps,conn);
                }
                SurveyLog.debug(et);
            } else {
                readonly();
            }

            internalSetVoteForValue(user, distinguishingXpath, value, null, xmlsource); // will create/throw away a resolver.
        }

        /**
         * @param user
         * @param distinguishingXpath
         * @param value
         * @param source
         * @return
         */
        private final VoteResolver<String> internalSetVoteForValue(User user,
                String distinguishingXpath, String value, VoteResolver<String> resolver, DataBackedSource source) {
            if(value!=null) {
                getXpathToVotes(distinguishingXpath).put(user, value);
            } else {
                getXpathToVotes(distinguishingXpath).remove(user); 
                allVoters.add(user);
            }
            stamp.next();
            return resolver=source.setValueFromResolver(distinguishingXpath, resolver);
        }

		@Override
        public boolean userDidVote(User myUser, String somePath) {
            Map<User, String> x = getXpathToVotes(somePath);
            if(x==null) return false;
            if(x.containsKey(myUser)) return true;
            //if(allVoters.contains(myUser)) return true; // voted for null
            return false;
        }

        public TestResultBundle getTestResultData(Map<String,String> options) {
            return gTestCache.getBundle(locale, options);
        }
    }

    /**
     * @author srl
     *
     */
    public class DelegateXMLSource extends XMLSource {
        protected XMLSource delegate;

        public DelegateXMLSource(CLDRLocale locale) {			
            setLocaleID(locale.getBaseName());

            delegate=sm.getDiskFactory().makeSource(locale.getBaseName());
        }
        public DelegateXMLSource(XMLSource source) {			
            setLocaleID(source.getLocaleID());
            delegate = source;
        }


        /* (non-Javadoc)
         * @see com.ibm.icu.util.Freezable#freeze()
         */
        @Override
        public Object freeze() {
            readonly();
            return null;
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#getFullPathAtDPath(java.lang.String)
         */
        @Override
        public String getFullPathAtDPath(String path) {
            return delegate.getFullPathAtDPath(path);
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#getValueAtDPath(java.lang.String)
         */
        @Override
        public String getValueAtDPath(String path) {
            String v =  delegate.getValueAtDPath(path);
            //SurveyLog.logger.warning("@@@@ ("+this.getLocaleID()+")" + path+"="+v);
            return v;
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#getXpathComments()
         */
        @Override
        public Comments getXpathComments() {
            return delegate.getXpathComments();
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#iterator()
         */
        @Override
        public Iterator<String> iterator() {
            return delegate.iterator();
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#putFullPathAtDPath(java.lang.String, java.lang.String)
         */
        @Override
        public void putFullPathAtDPath(String distinguishingXPath,
                String fullxpath) {
            readonly();
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#putValueAtDPath(java.lang.String, java.lang.String)
         */
        @Override
        public void putValueAtDPath(String distinguishingXPath, String value) {
            readonly();
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#removeValueAtDPath(java.lang.String)
         */
        @Override
        public void removeValueAtDPath(String distinguishingXPath) {
            readonly();
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#setXpathComments(org.unicode.cldr.util.XPathParts.Comments)
         */
        @Override
        public void setXpathComments(Comments comments) {
            readonly();
        }
        @Override
        public void getPathsWithValue(String valueToMatch, String pathPrefix, Set<String> result) {
            delegate.getPathsWithValue(valueToMatch, pathPrefix, result);
            
        }

        //		/* (non-Javadoc)
        //		 * @see org.unicode.cldr.util.XMLSource#make(java.lang.String)
        //		 */
        //		@Override
        //		public XMLSource make(String localeID) {
        //			return makeSource(localeID, this.isResolving());
        //		}

        //		/* (non-Javadoc)
        //		 * @see org.unicode.cldr.util.XMLSource#getAvailableLocales()
        //		 */
        //		@SuppressWarnings("rawtypes")
        //		@Override
        //		public Set getAvailableLocales() {
        //			return handleGetAvailable();
        //		}



        //		@Override
        //		protected synchronized TreeMap<String, String> getAliases() {
        //			if(true) throw new InternalError("NOT IMPLEMENTED.");
        //			return null;
        //		}

    }

    // Database stuff here.
    static final String CLDR_VBV = "cldr_votevalue";

    /**
     * These locales can not be modified.
     */
    private static final CLDRLocale readOnlyLocales[] = { 
    		CLDRLocale.getInstance("root"), 
    		CLDRLocale.getInstance("en"), 
    		CLDRLocale.getInstance("en_ZZ")
    	};


    //private static final String SOME_KEY = "//ldml/localeDisplayNames/keys/key[@type=\"collation\"]";

    /**
     * Is this a locale that can't be modified?
     * @param loc
     * @return
     */
    public static final boolean isReadOnlyLocale(CLDRLocale loc) {
        for(int i=0;i<readOnlyLocales.length;i++) {
            if(readOnlyLocales[i] == loc) return true;
        }
        return false;
    }

    /**
     * Is this a locale that can't be modified?
     * @param loc
     * @return
     */
    public static final boolean isReadOnlyLocale(String loc) {
    	return isReadOnlyLocale(CLDRLocale.getInstance(loc));
    }

    private static void readonly() {
        throw new InternalError("This is a readonly instance.");
    }

    /**
     * Throw an error. 
     */
    static public void unimp() {
        throw new InternalError("NOT YET IMPLEMENTED - TODO!.");
    }

    boolean dbIsSetup = false;

    /**
     * Per locale map
     */
    private Map<CLDRLocale,PerLocaleData> locales = new HashMap<CLDRLocale,PerLocaleData>();
    
    /**
     * Test cache
     */
    TestCache gTestCache = new SimpleTestCache();

    /**
     * The infamous back-pointer.
     */
    public SurveyMain sm = null;

    private org.unicode.cldr.util.PathHeader.Factory phf;

    /**
     * Construct one.
     */
    public STFactory(SurveyMain sm) {
        super();
        this.sm = sm;
        setSupplementalDirectory(sm.getDiskFactory().getSupplementalDirectory());
        
        gTestCache.setFactory(this, "(?!.*(CheckCoverage).*).*", sm.getBaselineFile());
        sm.reg.addListener(this);
        handleUserChanged(null);
        phf = PathHeader.getFactory(sm.getBaselineFile());
        surveyMenus= new SurveyMenus(this, phf);
    }

    @Override
    public BallotBox<User> ballotBoxForLocale(CLDRLocale locale) {
        return get(locale);
    }
    
    public Stamp stampForLocale(CLDRLocale locale) {
        return get(locale).getStamp();
    }
    
    /**
     * Fetch a locale from the per locale data, create if not there. 
     * @param locale
     * @return
     */
    private final PerLocaleData get(CLDRLocale locale) { 
        PerLocaleData pld = locales.get(locale);
        if(pld==null) {
            pld = new PerLocaleData(locale);
            locales.put(locale, pld);
        }
        return pld;
    }

    private final PerLocaleData get(String locale) {
        return get(CLDRLocale.getInstance(locale));
    }

    @SuppressWarnings("unchecked")
    public TestCache.TestResultBundle getTestResult(CLDRLocale loc, Map<String,String> options) {
        return get(loc).getTestResultData(options);
    }

    public ExampleGenerator getExampleGenerator() {
        CLDRFile fileForGenerator = sm.getBaselineFile();

        if(fileForGenerator==null) {
            SurveyLog.logger.warning("Err: fileForGenerator is null for " );
        }
        ExampleGenerator exampleGenerator = new ExampleGenerator(fileForGenerator, sm.getBaselineFile(), SurveyMain.fileBase + "/../supplemental/");
        exampleGenerator.setVerboseErrors(sm.twidBool("ExampleGenerator.setVerboseErrors"));
        return exampleGenerator;
    }

    /* (non-Javadoc)
     * @see org.unicode.cldr.util.Factory#getMinimalDraftStatus()
     */
    @Override
    protected DraftStatus getMinimalDraftStatus() {
        return DraftStatus.unconfirmed;
    }

    /* (non-Javadoc)
     * @see org.unicode.cldr.util.Factory#getSourceDirectory()
     */
    @Override
    public File[] getSourceDirectories() {
        return sm.getDiskFactory().getSourceDirectories();
    }

    @Override
    public File getSourceDirectoryForLocale(String localeID) {
        return sm.getDiskFactory().getSourceDirectoryForLocale(localeID);
    }

    /* (non-Javadoc)
     * @see org.unicode.cldr.util.Factory#handleGetAvailable()
     */
    @Override
    protected Set<String> handleGetAvailable() {
        return sm.getDiskFactory().getAvailable();
    }
    
    private Map<CLDRLocale,Set<CLDRLocale>> subLocaleMap = new HashMap<CLDRLocale,Set<CLDRLocale>>();
    Set<CLDRLocale> allLocales = null;
    private Set<CLDRLocale> getAvailableCLDRLocales() {
        if(allLocales==null) {
            allLocales =  CLDRLocale.getInstance(getAvailable());
        }
        return allLocales;
    }
    public Set<CLDRLocale> subLocalesOf(CLDRLocale forLocale) {
        Set<CLDRLocale> result = subLocaleMap.get(forLocale);
        if(result==null) {
            result = calculateSubLocalesOf(forLocale, getAvailableCLDRLocales());
            subLocaleMap.put(forLocale, result);
        }
        return result;
    }

    private Set<CLDRLocale> calculateSubLocalesOf(CLDRLocale locale, Set<CLDRLocale> available) {
        Set<CLDRLocale> sub = new TreeSet<CLDRLocale>();
        for(CLDRLocale l : available) {
            if(l.getParent() == locale) {
                sub.add(l);
            }
        }
        return sub;
    }


    /* (non-Javadoc)
     * @see org.unicode.cldr.util.Factory#handleMake(java.lang.String, boolean, org.unicode.cldr.util.CLDRFile.DraftStatus)
     */
    @Override
    protected CLDRFile handleMake(String localeID,
            boolean resolved,
            DraftStatus madeWithMinimalDraftStatus) {
        return get(localeID).getFile(resolved);
    }

    public CLDRFile make(CLDRLocale loc, boolean resolved) {
        return make(loc.getBaseName(),resolved);
    }


    public XMLSource makeSource(String localeID, boolean resolved) {
        if(localeID==null) return null; // ?!
        return get(localeID).makeSource(resolved);
    }

    /**
     * Prepare statement.  
     * Args: locale
     * Result: xpath,submitter,value
     * @param conn
     * @return
     * @throws SQLException
     */
    private PreparedStatement openQueryByLocale(Connection conn) throws SQLException {
        setupDB();
        return DBUtils.prepareForwardReadOnly(conn, "SELECT xpath,submitter,value FROM " + CLDR_VBV + " WHERE locale = ?");
    }

    private synchronized final void setupDB() 
    {
        if(dbIsSetup) return;
        dbIsSetup=true; // don't thrash.
        Connection conn = null;
        String sql ="(none)"; // this points to 
        Statement s = null;
        try {
            conn = DBUtils.getInstance().getDBConnection();

            boolean isNew = !DBUtils.hasTable(conn, CLDR_VBV);
            if(!isNew) {
                return; // nothing to setup
            }

            /*				
				    CREATE TABLE  cldr_votevalue (
				        locale VARCHAR(20),
				        xpath  INT NOT NULL,
				        submitter INT NOT NULL,
				        value BLOB    
				     );

				     CREATE UNIQUE INDEX cldr_votevalue_unique ON cldr_votevalue (locale,xpath,submitter);
             */
            s = conn.createStatement();

            sql = "create table " + CLDR_VBV + "( " +
                    "locale VARCHAR(20), " + 
                    "xpath  INT NOT NULL, " +
                    "submitter INT NOT NULL, " +
                    "value "+DBUtils.DB_SQL_UNICODE+", " +
                    DBUtils.DB_SQL_LAST_MOD + " " +
                    ", PRIMARY KEY (locale,submitter,xpath) " +

			" )";
            //            SurveyLog.logger.info(sql);
            s.execute(sql);

            sql = "CREATE UNIQUE INDEX  " + CLDR_VBV + " ON cldr_votevalue (locale,xpath,submitter)";
            s.execute(sql);
            s.close();
            s = null; //don't close twice.
            conn.commit();
        } catch(SQLException se) {
            SurveyLog.logException(se, "SQL: " + sql);
            SurveyMain.busted("Setting up DB for STFactory, SQL: "  + sql, se);
            throw new InternalError("Setting up DB for STFactory, SQL: " + sql);
        } finally {
            DBUtils.close(s,conn);
        }
    }

    /**
     * Close and re-open the factory. For testing only!
     * @return
     */
    public STFactory TESTING_shutdownAndRestart() {
        sm.TESTING_removeSTFactory();
        return sm.getSTFactory();
    }

	@Override
	public synchronized void handleUserChanged(User u) {
        VoteResolver.setVoterToInfo(sm.reg.getVoterToInfo());	
	}
	
	private Set<String> badPaths = new HashSet<String>();
	
    public final synchronized PathHeader getPathHeader(String xpath) {
        if(!badPaths.contains(xpath)) {
            try {
                return phf.fromPath(xpath);
            } catch (Throwable t) {
                SurveyLog.logException(t,"PH for path " + xpath + " (will show this once)");
                badPaths.add(xpath);
            }
        }
        return null;
    }
    
    private SurveyMenus surveyMenus;
    
    public final SurveyMenus getSurveyMenus() {
        return surveyMenus;
    }

    /**
     * Resolving old file, or null if none.
     * @param locale
     * @return
     */
    public CLDRFile getOldFile(CLDRLocale locale) {
        return get(locale).getOldFile();
    }
}
