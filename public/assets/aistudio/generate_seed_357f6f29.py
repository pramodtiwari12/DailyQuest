import csv, os

# Balanced synthetic starter corpus: 50 semantic task families per category,
# expressed in English, Devanagari Hindi, and Romanized Hinglish.
# These are seed examples, not a claim of real-world or externally validated data.
CATEGORIES = {
'ST': ('Study', [
 ('Read','पढ़ो','padho'),('Revise','दोहराओ','dohrao'),('Practice','अभ्यास करो','abhyaas karo'),('Summarize','सार लिखो','saar likho'),('Prepare for','की तैयारी करो','ki taiyari karo')], [
 ('a mathematics chapter','गणित का अध्याय','ganit ka chapter'),('physics formulas','भौतिकी के सूत्र','physics ke formulas'),('chemistry reactions','रसायन विज्ञान की अभिक्रियाएँ','chemistry ki reactions'),('lecture notes','कक्षा के नोट्स','class ke notes'),('calculus problems','कलन के सवाल','calculus ke sawal'),('linear algebra exercises','रैखिक बीजगणित के प्रश्न','linear algebra ke questions'),('an English essay','अंग्रेज़ी का निबंध','English ka essay'),('a history chapter','इतिहास का अध्याय','history ka chapter'),('exam questions','परीक्षा के प्रश्न','exam ke questions'),('important definitions','महत्वपूर्ण परिभाषाएँ','important definitions')]),
'WC': ('Work/Career', [
 ('Draft','का मसौदा बनाओ','ka draft banao'),('Review','की समीक्षा करो','ka review karo'),('Prepare','की तैयारी करो','ki taiyari karo'),('Update','अपडेट करो','update karo'),('Submit','जमा करो','submit karo')], [
 ('a professional email','एक professional email','ek professional email'),('my resume','अपना resume','apna resume'),('a job application','job application','job application'),('interview answers','interview ke answers','interview ke answers'),('my portfolio','apna portfolio','apna portfolio'),('a project proposal','project proposal','project proposal'),('meeting notes','meeting notes','meeting notes'),('a presentation deck','presentation deck','presentation deck'),('my LinkedIn profile','apni LinkedIn profile','apni LinkedIn profile'),('the weekly work report','weekly work report','weekly work report')]),
'HF': ('Health & Fitness', [
 ('Complete','पूरा करो','poora karo'),('Do','करो','karo'),('Track','का रिकॉर्ड रखो','ka record rakho'),('Plan','की योजना बनाओ','ki planning karo'),('Improve','बेहतर बनाओ','better banao')], [
 ('a morning workout','सुबह की कसरत','subah ki workout'),('a 5 km run','5 किलोमीटर की दौड़','5 km ki running'),('a stretching routine','stretching routine','stretching routine'),('a set of push-ups','push-ups का एक set','push-ups ka ek set'),('pull-up practice','pull-up practice','pull-up practice'),('a yoga session','yoga session','yoga session'),('my daily step count','रोज़ के steps','daily steps'),('a protein-rich meal','protein वाला भोजन','protein wali meal'),('my water intake','पानी पीने का लक्ष्य','paani peene ka target'),('my sleep schedule','सोने का समय','sone ka time')]),
'SC': ('Skill/Coding', [
 ('Write','लिखो','likho'),('Debug','की debugging करो','ki debugging karo'),('Practice','की practice करो','ki practice karo'),('Build','बनाओ','banao'),('Review','का review करो','ka review karo')], [
 ('Python functions','Python functions','Python functions'),('C++ arrays','C++ arrays','C++ arrays'),('a LeetCode problem','LeetCode का एक सवाल','LeetCode ka ek question'),('SQL queries','SQL queries','SQL queries'),('an ML notebook','ML notebook','ML notebook'),('a Flutter screen','Flutter screen','Flutter screen'),('Git commands','Git commands','Git commands'),('a neural network','neural network','neural network'),('a REST API endpoint','REST API endpoint','REST API endpoint'),('a data structures exercise','data structures का सवाल','data structures ka question')]),
'FI': ('Finance', [
 ('Track','का हिसाब रखो','ka hisaab rakho'),('Review','की समीक्षा करो','ka review karo'),('Plan','की योजना बनाओ','ki planning karo'),('Save for','के लिए बचत करो','ke liye saving karo'),('Categorize','को श्रेणियों में बाँटो','ko categories mein baanto')], [
 ('monthly expenses','महीने के खर्चे','monthly expenses'),('my budget','अपना budget','apna budget'),('my bank balance','अपना bank balance','apna bank balance'),('a savings goal','बचत का लक्ष्य','saving ka goal'),('subscription payments','subscription payments','subscription payments'),('travel costs','यात्रा का खर्च','travel ka kharcha'),('daily spending','रोज़ का खर्च','daily kharcha'),('an emergency fund','emergency fund','emergency fund'),('investment notes','investment notes','investment notes'),('upcoming bills','आने वाले bills','aane wale bills')]),
'FS': ('Family & Social', [
 ('Call','को फोन करो','ko phone karo'),('Message','को message भेजो','ko message bhejo'),('Plan','की योजना बनाओ','ki planning karo'),('Visit','से मिलने जाओ','se milne jao'),('Thank','का आभार मानो','ka shukriya ada karo')], [
 ('my parents','अपने माता-पिता','apne parents'),('a close friend','एक करीबी दोस्त','ek close friend'),('my sibling','अपने भाई या बहन','apne bhai ya behen'),('a relative','एक रिश्तेदार','ek relative'),('a college friend','college के दोस्त','college ke dost'),('a mentor','एक mentor','ek mentor'),('a family dinner','परिवार के साथ dinner','family ke saath dinner'),('a weekend outing','weekend की outing','weekend ki outing'),('a birthday gathering','birthday की gathering','birthday ki gathering'),('a helpful classmate','मदद करने वाले classmate','helpful classmate')]),
'MS': ('Mindfulness/Self-care', [
 ('Practice','का अभ्यास करो','ki practice karo'),('Complete','पूरा करो','poora karo'),('Write','लिखो','likho'),('Reflect on','पर विचार करो','par reflect karo'),('Follow','का पालन करो','follow karo')], [
 ('10 minutes of meditation','10 मिनट ध्यान','10 minute meditation'),('deep breathing','गहरी साँसों का अभ्यास','deep breathing'),('a gratitude journal entry','gratitude journal की entry','gratitude journal entry'),('a mindful walk','ध्यान से टहलना','mindful walk'),('a screen-free break','screen-free break','screen-free break'),('my evening wind-down routine','शाम का आराम वाला routine','evening wind-down routine'),('a daily reflection','दिन की समीक्षा','daily reflection'),('a positive affirmation','सकारात्मक affirmation','positive affirmation'),('a digital detox session','digital detox session','digital detox session'),('a self-care routine','self-care routine','self-care routine')]),
'HC': ('Home & Chores', [
 ('Clean','साफ करो','saaf karo'),('Organize','व्यवस्थित करो','organize karo'),('Wash','धोओ','dhoo'),('Repair','ठीक करो','theek karo'),('Restock','दोबारा भर लो','dobara bhar lo')], [
 ('my study desk','अपनी study table','apni study table'),('the bedroom','बेडरूम','bedroom'),('the laundry','कपड़े','kapde'),('the kitchen shelf','रसोई की shelf','kitchen ki shelf'),('my backpack','अपना bag','apna bag'),('the bathroom','बाथरूम','bathroom'),('my weekly groceries','साप्ताहिक किराने का सामान','weekly groceries'),('the water bottle','पानी की bottle','paani ki bottle'),('the room dustbin','कमरे का dustbin','room ka dustbin'),('my wardrobe','अपनी अलमारी','apni wardrobe')]),
'CH': ('Creativity/Hobby', [
 ('Sketch','का sketch बनाओ','ka sketch banao'),('Compose','compose करो','compose karo'),('Edit','edit करो','edit karo'),('Practice','का अभ्यास करो','ki practice karo'),('Create','बनाओ','banao')], [
 ('a portrait','एक portrait','ek portrait'),('a short music loop','छोटा music loop','short music loop'),('a video sequence','video sequence','video sequence'),('a photography concept','photography concept','photography concept'),('a digital illustration','digital illustration','digital illustration'),('a guitar exercise','guitar exercise','guitar exercise'),('a short story','छोटी कहानी','short story'),('a poster layout','poster layout','poster layout'),('a new melody','नई धुन','nayi melody'),('a personal blog draft','personal blog का draft','personal blog ka draft')]),
'RE': ('Rest/Entertainment', [
 ('Watch','देखो','dekho'),('Play','खेलो','khelo'),('Listen to','सुनो','suno'),('Read','पढ़ो','padho'),('Take a break with','के साथ break लो','ke saath break lo')], [
 ('a documentary','एक documentary','ek documentary'),('a casual mobile game','casual mobile game','casual mobile game'),('a music playlist','music playlist','music playlist'),('a comic chapter','comic का chapter','comic ka chapter'),('a comedy episode','comedy episode','comedy episode'),('a nature video','nature video','nature video'),('a podcast episode','podcast episode','podcast episode'),('a relaxing playlist','relaxing playlist','relaxing playlist'),('a short leisure walk','आराम से छोटी walk','aaram se short walk'),('a favorite movie scene','पसंदीदा movie scene','favorite movie scene')]),
'OT': ('Other', [
 ('Handle','का काम निपटाओ','ka kaam niptao'),('Check','जाँचो','check karo'),('Review','की समीक्षा करो','ka review karo'),('Organize','व्यवस्थित करो','organize karo'),('Complete','पूरा करो','poora karo')], [
 ('an important personal errand','ज़रूरी निजी काम','zaroori personal kaam'),('a pending form','pending form','pending form'),('an appointment detail','appointment की जानकारी','appointment ki details'),('travel documents','यात्रा के documents','travel documents'),('a delivery update','delivery update','delivery update'),('an important reminder','ज़रूरी reminder','zaroori reminder'),('my weekly checklist','अपनी weekly checklist','apni weekly checklist'),('a miscellaneous task','एक अन्य काम','ek doosra kaam'),('a personal admin task','personal admin task','personal admin task'),('a list of pending items','बाकी कामों की list','baaki kaamon ki list')]),
}

difficulty = ['Easy', 'Medium', 'Hard', 'Epic', 'Medium']
durations = [15, 30, 45, 60, 90, 120]
rows=[]
for code,(category, actions, objects) in CATEGORIES.items():
    for ai,(verb_en,verb_hi,verb_hinglish) in enumerate(actions):
        for oi,(obj_en,obj_hi,obj_hinglish) in enumerate(objects):
            family=f'{code}-{ai+1:02d}-{oi+1:02d}'
            duration=durations[(ai*2 + oi) % len(durations)]
            diff=difficulty[ai]
            variants = [
                ('en', f'{verb_en} {obj_en}'),
                ('hi', f'{obj_hi} {verb_hi}'),
                ('hinglish', f'{obj_hinglish} {verb_hinglish}')
            ]
            for lang,title in variants:
                rows.append({
                    'example_id': f'{family}-{lang}',
                    'family_id': family,
                    'task_title': title,
                    'category': category,
                    'difficulty': diff,
                    'estimated_minutes': duration,
                    'language': lang,
                    'source_type': 'synthetic_seed',
                    'notes': 'Synthetic starter example; validate and augment with reviewed opt-in examples before claiming production accuracy.'
                })

out='/mnt/data/dayquest_assets/ml/seed.csv'
with open(out,'w',newline='',encoding='utf-8-sig') as f:
    w=csv.DictWriter(f, fieldnames=list(rows[0].keys()))
    w.writeheader(); w.writerows(rows)

# Time parser acceptance cases: parser should resolve values relative to a fixed test clock/time zone.
parser_cases = [
('gym by 6pm','en','18:00',None,0,'ByTime','Explicit 12-hour time'),
('study physics 2 hours','en',None,120,0,'Anytime','Duration only'),
('revise calculus for 45 minutes','en',None,45,0,'Anytime','Duration phrase'),
('finish assignment by 8:30 pm','en','20:30',None,0,'ByTime','Colon time'),
('workout at 7 am','en','07:00',None,0,'ByTime','At-time expression'),
('read for 15 min','en',None,15,0,'Anytime','Abbreviated duration'),
('clean room in 30 minutes','en',None,None,0,'Ambiguous','Relative due time vs task duration is ambiguous; ask or surface interpretation'),
('submit form tomorrow by 10am','en','10:00',None,1,'ByTime','Relative date and due time'),
('call parents at 8 tonight','en','20:00',None,0,'ByTime','Contextual evening'),
('practice coding for 2.5 hours','en',None,150,0,'Anytime','Decimal duration'),
('shaam 6 baje tak gym','hinglish','18:00',None,0,'ByTime','Hindi time phrase'),
('subah 7 baje walk','hinglish','07:00',None,0,'ByTime','Morning time'),
('2 ghante physics padhna hai','hinglish',None,120,0,'Anytime','Hinglish duration'),
('30 minute coding practice','hinglish',None,30,0,'Anytime','Hinglish minutes'),
('kal 9 baje assignment submit','hinglish','09:00',None,1,'ByTime','Tomorrow due time'),
('raat 8 baje tak call karna','hinglish','20:00',None,0,'ByTime','Night phrase'),
('aadha ghanta meditation','hinglish',None,30,0,'Anytime','Half hour'),
('dedh ghanta ML padhna','hinglish',None,90,0,'Anytime','One and a half hours'),
('parso 6 pm meeting prepare','hinglish','18:00',None,2,'ByTime','Day after tomorrow'),
('dopahar 1 baje lunch prep','hinglish','13:00',None,0,'ByTime','Afternoon'),
('शाम 6 बजे तक जिम','hi','18:00',None,0,'ByTime','Devanagari time'),
('सुबह 7 बजे दौड़ना','hi','07:00',None,0,'ByTime','Devanagari morning'),
('2 घंटे भौतिकी पढ़नी है','hi',None,120,0,'Anytime','Devanagari duration'),
('30 मिनट ध्यान करना','hi',None,30,0,'Anytime','Devanagari minutes'),
('कल सुबह 9 बजे तक रिपोर्ट','hi','09:00',None,1,'ByTime','Relative date and deadline'),
('रात 8 बजे फोन करना','hi','20:00',None,0,'ByTime','Evening time'),
('डेढ़ घंटे coding practice','hi',None,90,0,'Anytime','Mixed-script duration'),
('परसों शाम 6 बजे मीटिंग','hi','18:00',None,2,'ByTime','Day after tomorrow'),
('by 6','en',None,None,0,'Ambiguous','AM/PM ambiguous; ask/confirm rather than silently guessing'),
('tomorrow','en',None,None,1,'Anytime','Date-only phrase'),
('in 20 minutes','en',None,None,0,'Ambiguous','Could be reminder offset or deadline; do not silently reinterpret'),
('next Monday at 9:15','en','09:15',None,None,'ByTime','Next weekday resolved by test helper from a fixed anchor date'),
('shaam ko padhna','hinglish',None,None,0,'Anytime','No exact time: do not invent one'),
('2 ghante baad reminder','hinglish',None,None,0,'Anytime','Relative reminder, not task duration; request clarification in UI'),
('by 6:30 PM','en','18:30',None,0,'ByTime','Explicit time'),
('7:05 am walk','en','07:05',None,0,'ByTime','Minutes precision'),
('120 min DSA','en',None,120,0,'Anytime','Numeric minutes'),
('quarter hour journal','en',None,15,0,'Anytime','Quarter hour'),
('ek ghanta workout','hinglish',None,60,0,'Anytime','One hour'),
('paune saat baje call','hinglish',None,None,0,'Ambiguous','06:45 vs 18:45 ambiguous without context; ask/confirm'),
('sawa 8 baje tak kaam','hinglish',None,None,0,'Ambiguous','08:15 vs 20:15 ambiguous without context; ask/confirm'),
('रात 10:30 तक पढ़ाई','hi','22:30',None,0,'ByTime','Late night explicit'),
('दोपहर 2 से 3 बजे पढ़ना','hi',None,None,0,'Range','Start-end time range'),
('work from 4 to 5 pm','en',None,None,0,'Range','Start-end time range'),
('gym 6-7pm','en',None,None,0,'Range','Compact time range'),
('kal 2 se 3 baje study','hinglish',None,None,1,'Range','Relative date range'),
('every weekday at 7 am run','en','07:00',None,0,'ByTime','Repeat intent should be parsed separately'),
('roz subah 6 baje walk','hinglish','06:00',None,0,'ByTime','Daily recurrence'),
('हर रविवार 10 बजे revision','hi','10:00',None,0,'ByTime','Weekly recurrence'),
('read for 1h 20m','en',None,80,0,'Anytime','Compound duration'),
('study 1 hour 30 minutes','en',None,90,0,'Anytime','Compound duration'),
]
with open('/mnt/data/dayquest_assets/ml/time_parser_cases.csv','w',newline='',encoding='utf-8-sig') as f:
    w=csv.writer(f)
    w.writerow(['input_text','language','expected_time_local_24h','expected_duration_minutes','date_offset_days','expected_schedule_type','test_note'])
    w.writerows(parser_cases)

print(f'seed rows: {len(rows)}; category counts:')
from collections import Counter
print(Counter(r['category'] for r in rows))
print('parser cases:',len(parser_cases))
