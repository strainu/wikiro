﻿#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Add {{Monument istoric}} to all the images that illustrate the list, 
but don't have the template in commons.
'''

import sys, time, warnings, json, string, re
sys.path.append("..")
import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user
from pywikibot import catlib as catlib


_log = "link2.err.log"
_flog = None

def initLog():
	global _flog, _log;
	_flog = open(_log, 'w+')

def closeLog():
	global _flog
	_flog.close()

def log(string):
	pywikibot.output(string.encode("utf8") + "\n")
	_flog.write(string.encode("utf8") + "\n")

def commonsImage(image):
	site = pywikibot.getSite('commons', 'commons')
        try:
                page = pywikibot.Page(site, image)
		if not page.exists():
			return None
                if page.isRedirectPage():
                        page = page.getRedirectTarget()

        except Exception:
                return None
	return page

def localImage(image):
	site = pywikibot.getSite('ro', 'wikipedia')
	try:
		page = pywikibot.Page(site, image)
		if not page.exists():
			return None
		if page.isRedirectPage():
			page = page.getRedirectTarget()
	except Exception as e:
		return None
	return page
	
def list2commons(db):
	#this is the big loop that should only happen once
	for monument in db:
		rawCode = monument["Cod"].strip()
		regexp = re.compile("(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2,3})?))", re.I)
		result = re.findall(regexp, rawCode)
		if len(result) > 0:
			code = result[0][0]
		else:
			code = rawCode
		pywikibot.output(code)
		
		image = monument["Imagine"]
		if image == None or image.strip() == "":
			continue
			
		image = image.replace(u'Fișier', u'File')
		image = image.replace(u'Imagine', u'File')
		#print image
		page = commonsImage(image)
		#print page
		if page == None:
                	log("I: Local image [[:%s]] for code %s" % (image, code))
			page = localImage(image)
		if page == None:
			log("E: Local image [[:%s]] for code %s does not exist" % (image, code))
			continue	
		try:
			text = page.get()
		except Exception:
			continue
		#print page.site
		templates = page.templatesWithParams()
		info_params = None
		found = 0
		for (template, params) in templates:
			#print template
			if unicode(template.title(withNamespace = False)) == u"Monument istoric" or unicode(template.title(withNamespace = False)) == u"CodLMI":
				found = 1
				if code <> str(params[0]):
					log(u"W: Nepotrivire între codul din listă (%s) și unul din codurile din imagine (%s)" % (code, params[0]))
			elif unicode(template.title(withNamespace = False)) == u"Information" or unicode(template.title(withNamespace = False)) == u"Informații":
				pass
				#info_params = params
				#print params
				
		if not found: # we need to add the template
			#print text
			addedText = "{{Monument istoric|%s}}" % code
			if info_params == None:
				text = addedText + "\n" + text
				#print text
				page.put(text, comment="Adding {{tl|Monument istoric}}")
			else:
				description = None
				for param in info_params:
					pos = unicode(param.lower()).find(u"descri")
					if  pos == 0 or (pos > 0 and param[0:pos].strip() == ""):
						description = unicode(param)
						break
				if description:
					#print addedText
					new = description + addedText + "\n"
					#print new
					text = text.replace(description, new)
					#print text
					page.put(text, comment="Adding {{tl|Monument istoric}}")
				else:
					log(u"E: Imaginea pentru %s are formatul {{f|information}} dar nu și o descriere" % code)

def cat2commons(cat):
	start = True
	for code in cat:
		if len(cat[code]) > 1:
			log(u"E: Există mai multe categorii pentru %s" % code)
			continue
		name = cat[code][0]["name"]
		if name == u"Category:Cetatea Oradea":
			start = True
			continue
		if not start:
			continue
		pywikibot.output(u"Working on cat %s" % name)
		site = pywikibot.getSite('commons', 'commons')
		transGen = pagegenerators.CategorizedPageGenerator(catlib.Category(site, name))
		filteredGen = pagegenerators.NamespaceFilterPageGenerator(transGen, [6], site)
		pregenerator = pagegenerators.PreloadingGenerator(filteredGen, 50)
		for page in pregenerator:
			if page.isRedirectPage():
				page = page.getRedirectTarget()
			pywikibot.output(u"Working on file %s" % page.title())
			text = page.get()
			templates = page.templatesWithParams()
			found = False
			info_params = None
			for (template, params) in templates:
				if unicode(template.title(withNamespace = False)) == u"Monument istoric":
					found = True
					print params
					if code <> str(params[0]):
						log(u"W: Nepotrivire între codul din categorie (%s) și unul din codurile din imagine (%s)" % (code, params[0]))
				elif unicode(template.title(withNamespace = False)) == u"Information" or unicode(template.title(withNamespace = False)) == u"Artwork":
					pass
					#info_params = params
					#print params
			
			if found:
				continue   
			addedText = "{{Monument istoric|%s}}" % code
			if info_params == None:
				text = addedText + "\n" + text
				#print text
				page.put(text, comment="Adding {{tl|Monument istoric}}")
				continue
			else:
				description = None
			for param in info_params:
				pos = unicode(param.lower()).find(u"description")
				if pos == 0 or (pos > 0 and param[0:pos].strip() == ""):
					description = unicode(param)
					break
			if description:
				text = text.replace(description, description + addedText + "\n")
				#print text
				page.put(text, comment="Adding {{tl|Monument istoric}}")
			else:
				log(u"E: Imaginea pentru %s are formatul {{f|information}} dar nu și o descriere" % code)

def main():
	cat = None
	
	for arg in pywikibot.handleArgs():
		if arg.startswith('-cat'):
			cat = arg [len('-cat:'):]
	initLog()
	f = open("lmi_db.json", "r+")
	pywikibot.output("Reading database file...")
	db = json.load(f)
	pywikibot.output("...done")
	f.close();
	f = open("commons_Category_pages.json", "r+")
	pywikibot.output("Reading commons categories file...")
	categs = json.load(f)
	pywikibot.output("...done")
	f.close();
	if cat:
		for code in categs.keys():
			if categs[code][0][u"name"] != cat:
				del categs[code]
	print categs
	#list2commons(db)
	cat2commons(categs)
	closeLog()

if __name__ == "__main__":
	try:
		main()
	finally:
		pywikibot.stopme()