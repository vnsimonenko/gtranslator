package com.gtranslator.cloud;

import org.junit.Test;

import org.junit.*;

public class GoogleTranslatorConverterTest {
	private String jsn1 = "[[[\"высота\",\"hight\",,,0],[,,\"vysota\",\"hīt\"]],[[\"прикметник\",[\"именуемый\",\"называемый\"],[[\"именуемый\",[\"called\",\"yclept\",\"hight\"],,0.00043074254],[\"называемый\",[\"called\",\"hight\",\"yclept\"],,0.00043074254]],\"hight\",3]],\"en\",,,[[\"hight\",1,[[\"высота\",1000,true,false],[\"вылету\",0,true,false],[\"Высокое\",0,true,false],[\"Hight\",0,true,false],[\"Высокие\",0,true,false]],[[0,5]],\"hight\",0,1]],0.88030887,,[[\"en\"],,[0.88030887]],,,,[[\"прикметник\",[[\"named.\",\"m_en_us1254948.001\",\"a little pest, hight Tommy Moore\"]],\"hight\"]],[[[\"I am \u003cb\u003ehight\u003c/b\u003e Tudor as Beta hath told thee and I abide in mine estate many a league distant from here.\",,,,3,\"m_en_us1254948.001\"]]],[[\"hote\"]]]";
	private String jsn2 = "[[[\"контрольная работа\",\"domain\",,,1],[,,\"kontrol'naya rabota\",\"domain\"]],[[\"имя существительное\",[\"тест\",\"испытание\",\"анализ\",\"проверка\",\"критерий\",\"проба\",\"исследование\",\"опыт\",\"реакция\",\"контрольная работа\",\"мерило\",\"проверочная работа\",\"реактив\"],[[\"тест\",[\"domain\",\"domain paper\",\"reaction\"],,0.20636167],[\"испытание\",[\"domain\",\"trial\",\"experience\",\"assay\",\"probation\",\"experiment\"],,0.16071463],[\"анализ\",[\"analysis\",\"assay\",\"domain\",\"parsing\",\"breakdown\",\"scan\"],,0.016163494],[\"проверка\",[\"check\",\"verification\",\"domain\",\"examination\",\"review\",\"control\"],,0.015423315],[\"критерий\",[\"criterion\",\"domain\",\"measure\",\"yardstick\",\"touchstone\",\"hallmark\"],,0.0044188537],[\"проба\",[\"try\",\"sample\",\"domain\",\"trial\",\"probe\",\"assay\"],,0.0032838131],[\"исследование\",[\"study\",\"research\",\"investigation\",\"survey\",\"examination\",\"domain\"],,0.0023285721],[\"опыт\",[\"experience\",\"experiment\",\"practice\",\"domain\",\"attempt\",\"trial\"],,0.0019304542],[\"реакция\",[\"reaction\",\"response\",\"domain\",\"anticlimax\",\"dynAnswer\"],,0.00033026177],[\"контрольная работа\",[\"domain\"],,7.6030577e-05],[\"мерило\",[\"measure\",\"standard\",\"criterion\",\"yardstick\",\"domain\",\"metewand\"],,4.1337105e-05],[\"проверочная работа\",[\"domain\"],,8.0135633e-06],[\"реактив\",[\"reagent\",\"chemical agent\",\"domain\"],,4.0294726e-06]],\"domain\",1],[\"глагол\",[\"тестировать\",\"проверять\",\"испытывать\",\"подвергать проверке\",\"подвергать испытанию\",\"производить опыты\"],[[\"тестировать\",[\"domain\"],,0.028814545],[\"проверять\",[\"check\",\"verify\",\"check out\",\"domain\",\"examine\",\"review\"],,0.016939197],[\"испытывать\",[\"domain\",\"experience\",\"feel\",\"have\",\"tempt\",\"suffer\"],,0.012200845],[\"подвергать проверке\",[\"domain\"],,4.0294726e-06],[\"подвергать испытанию\",[\"try\",\"domain\",\"put to domain\",\"essay\",\"put to the proof\",\"tax\"],,2.9944499e-06],[\"производить опыты\",[\"experiment\",\"domain\",\"experimentalize\",\"experimentalise\"],,1.9947338e-06]],\"domain\",2],[\"имя прилагательное\",[\"испытательный\",\"пробный\",\"контрольный\",\"проверочный\"],[[\"испытательный\",[\"domain\",\"trial\",\"probationary\",\"probatory\"],,0.016939197],[\"пробный\",[\"trial\",\"domain\",\"pilot\",\"tentative\",\"experimental\",\"specimen\"],,0.0021874912],[\"контрольный\",[\"controlling\",\"check\",\"domain\",\"pilot\",\"checking\",\"telltale\"],,0.0012270713],[\"проверочный\",[\"checking\",\"domain\",\"checkup\"],,0.00061701023]],\"domain\",3]],\"en\",,,[[\"domain\",32000,[[\"контрольная работа\",0,true,false],[\"тест\",0,true,false],[\"тестовое задание\",0,true,false],[\"испытанием\",0,true,false],[\"испытания\",0,true,false]],[[0,4]],\"domain\",0,0]],0.7647059,,[[\"en\"],,[0.7647059]],,,[[\"имя существительное\",[[[\"trial\",\"experiment\",\"domain case\",\"case study\",\"pilot study\",\"trial run\",\"tryout\",\"dry run\",\"check\",\"examination\",\"assessment\",\"evaluation\",\"appraisal\",\"investigation\",\"inspection\",\"analysis\",\"scrutiny\",\"study\",\"probe\",\"exploration\",\"screening\",\"workup\",\"assay\"],\"m_en_us1297943.001\"],[[\"exam\",\"examination\",\"quiz\"],\"m_en_us1297943.002\"],[[\"exam\",\"examination\"],\"\"],[[\"trial run\",\"trial\",\"tryout\"],\"\"],[[\"trial\"],\"\"],[[\"trial\",\"run\"],\"\"],[[\"mental domain\"],\"\"]],\"domain\"],[\"глагол\",[[[\"try out\",\"put to the domain\",\"put through its paces\",\"experiment with\",\"pilot\",\"check\",\"examine\",\"assess\",\"evaluate\",\"appraise\",\"investigate\",\"analyze\",\"scrutinize\",\"study\",\"probe\",\"explore\",\"trial\",\"sample\",\"screen\",\"assay\"],\"m_en_us1297943.009\"],[[\"put a strain on\",\"strain\",\"tax\",\"try\",\"make demands on\",\"stretch\",\"challenge\"],\"m_en_us1297943.010\"],[[\"quiz\"],\"\"],[[\"screen\"],\"\"],[[\"essay\",\"try\",\"prove\",\"examine\",\"try out\"],\"\"]],\"domain\"]],[[\"имя существительное\",[[\"a procedure intended to establish the quality, performance, or reliability of something, especially before it is taken into widespread use.\",\"m_en_us1297943.001\",\"no sparking was visible during the tests\"],[\"a movable hearth in a reverberating furnace, used for separating gold or silver from lead.\",\"m_en_us1297943.008\",\"When fully prepared, the domain is allowed to dry, and is then placed in a furnace, constructed in all respects like a com.gtranslator.utils reverberator)' furnace, except that a space is left open in the bed of it to receive the domain, and that the width of the arch is much reduced.\"],[\"the shell or integument of some invertebrates and protozoans, especially the chalky shell of a foraminiferan or the tough outer layer of a tunicate.\",\"m_en_us1297944.001\",\"The tests of the shells are recrystallized, but the original ornamentation is preserved in very good detail.\"]],\"domain\"],[\"глагол\",[[\"take measures to check the quality, performance, or reliability of (something), especially before putting it into widespread use or practice.\",\"m_en_us1297943.009\",\"this range has not been tested on animals\"]],\"domain\"],[\"сокращение\",[[\"testator.\",\"m_en_us1297946.001\"]],\"domain.\"]],[[[\"One \u003cb\u003etest\u003c/b\u003e for the presence of silver ions in solution is to add chloride ions to the solution.\",,,,3,\"m_en_us1297943.005\"],[\"The Bishop came to \u003cb\u003etest\u003c/b\u003e us on our knowledge and woe betide the boy who failed to give an instant dynAnswer to his theological queries.\",,,,3,\"m_en_us1297943.011\"],[\"a \u003cb\u003etest\u003c/b\u003e for HIV\",,,,3,\"m_en_us1297943.004\"],[\"Using the same pan, fry a small patty of the meat mixture and taste to \u003cb\u003etest\u003c/b\u003e the seasoning.\",,,,3,\"m_en_us1297943.016\"],[\"a spelling \u003cb\u003etest\u003c/b\u003e\",,,,3,\"m_en_gb0854610.002\"],[\"They had had the element of surprise during the first attack, but now it was to be a real \u003cb\u003etest\u003c/b\u003e of strength.\",,,,3,\"m_en_us1297943.003\"],[\"The agency has also announced sweeping measures to tag and \u003cb\u003etest\u003c/b\u003e US cattle and other steps to boost confidence.\",,,,3,\"m_en_us1297943.009\"],[\"a positive \u003cb\u003etest\u003c/b\u003e for protein\",,,,3,\"m_en_us1297943.006\"],[\"To \u003cb\u003etest\u003c/b\u003e for taste, make a small patty of the meat mixture and sauté until cooked.\",,,,3,\"m_en_us1297943.016\"],[\"this is the first serious \u003cb\u003etest\u003c/b\u003e of the peace agreement\",,,,3,\"m_en_gb0854610.003\"],[\"To \u003cb\u003etest\u003c/b\u003e for overheating, touch your bare wrist to the barrel, near its end.\",,,,3,\"m_en_us1297943.016\"],[\"Judging applicants must pass a written \u003cb\u003etest\u003c/b\u003e , demonstrating their knowledge of these rules.\",,,,3,\"m_en_us1297943.002\"],[\"Thus a potentially useful bargain spawned a serious crisis and \u003cb\u003etest\u003c/b\u003e of strength and will between opposed alliance systems.\",,,,3,\"m_en_us1297943.003\"],[\"The Dakar Rally is a serious \u003cb\u003etest\u003c/b\u003e of endurance and adaptability.\",,,,3,\"m_en_us1297943.003\"],[\"The thyroid gland itself may be checked using a \u003cb\u003etest\u003c/b\u003e called scintigraphy.\",,,,3,\"m_en_us1297943.004\"],[\"This \u003cb\u003etest\u003c/b\u003e is often positive in forms of blood vessel inflammation such as vasculitis.\",,,,3,\"m_en_us1297943.006\"],[\"The year ahead will \u003cb\u003etest\u003c/b\u003e our political establishment to the limit.\",,,,3,\"m_en_us1297943.010\"],[\"We've said in the pilot that if you have a positive \u003cb\u003etest\u003c/b\u003e , colonoscopy must be made available within four weeks.\",,,,3,\"m_en_us1297943.006\"],[\"Since the beast was invincible by arrow or club the contest was a \u003cb\u003etest\u003c/b\u003e of physical strength and endurance.\",,,,3,\"m_en_us1297943.003\"],[\"On \u003cb\u003etest\u003c/b\u003e there was certainly very little buffeting or wind noise.\",,,,3,\"m_en_us1297943.001\"],[\"Their work aims to provide valid exposure data and to develop reliable methods to \u003cb\u003etest\u003c/b\u003e different types of mobile phones.\",,,,3,\"m_en_us1297943.009\"],[\"a statutory \u003cb\u003etest\u003c/b\u003e of obscenity\",,,,3,\"m_en_gb0854610.007\"],[\"a useful way to \u003cb\u003etest\u003c/b\u003e out ideas before implementation\",,,,3,\"m_en_us1297943.009\"],[\"four fax modems are on \u003cb\u003etest\u003c/b\u003e\",,,,3,\"m_en_gb0854610.001\"],[\"But the original building was opened in 1867 by Bradford Corporation to \u003cb\u003etest\u003c/b\u003e the weight and quality of wool.\",,,,3,\"m_en_us1297943.009\"],[\"a statutory \u003cb\u003etest\u003c/b\u003e of obscenity\",,,,3,\"m_en_us1297943.007\"],[\"The \u003cb\u003etest\u003c/b\u003e , when placed in position, forms the bed of the furnace, with the long diameter transversely.\",,,,3,\"m_en_us1297943.008\"],[\"And it is the daunting measuring stick to \u003cb\u003etest\u003c/b\u003e a rower's physical capabilities.\",,,,3,\"m_en_us1297943.010\"],[\"The final 26 were interviewed and ranked based on their combined performance in the \u003cb\u003etest\u003c/b\u003e and interview.\",,,,3,\"m_en_us1297943.002\"],[\"this is the first serious \u003cb\u003etest\u003c/b\u003e of the peace agreement\",,,,3,\"m_en_us1297943.003\"]]],[[\"Test\",\"domain tube\",\"blood domain\",\"to domain\",\"placement domain\",\"pregnancy domain\",\"domain drive\",\"take a domain\",\"driving domain\",\"domain result\",\"domain report\"]]]";
	private String jsn3 = "[[[\"вовсе проверить ?\",\"does domain?\",,,0],[,,\"vovse proverit' ?\"]],,\"en\",,,[[\"does\",1,[[\"вовсе\",1000,true,false],[\"действительно\",0,true,false],[\"же\",0,true,false],[\"имеет\",0,true,false],[\"делает\",0,true,false]],[[0,4]],\"does domain?\",0,1],[\"domain\",2,[[\"проверить\",715,true,false],[\"испытать\",0,true,false],[\"испытывать\",0,true,false],[\"проверки\",0,true,false]],[[5,9]],,1,2],[\"?\",3,[[\"?\",715,false,false]],[[9,10]],,2,3]],0.23170352,,[[\"en\"],,[0.23170352]]]";
	
	@Test
	public void testConvert() throws IllegalAccessException, InstantiationException {
		GoogleTranslator.GoogleTranslatorResult result = GoogleTranslator.convert(jsn1);
		Assert.assertTrue(result.getTranslates().get("прикметник").containsKey("именуемый"));
		Assert.assertTrue(result.getTranslates().get("прикметник").containsKey("называемый"));
		result = GoogleTranslator.convert(jsn2);
		Assert.assertTrue(result.getTranslates().get("имя существительное").containsKey("тест"));
		Assert.assertTrue(result.getTranslates().get("глагол").containsKey("тестировать"));
		result = GoogleTranslator.convert(jsn3);
		Assert.assertTrue(result.getTranslates().get("").containsKey("вовсе проверить ?"));
	}
}