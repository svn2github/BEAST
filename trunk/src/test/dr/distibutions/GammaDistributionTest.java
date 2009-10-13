package test.dr.distibutions;

import dr.math.distributions.GammaDistribution;
import dr.math.functionEval.GammaFunction;
import junit.framework.TestCase;

public class GammaDistributionTest extends TestCase{

	GammaDistribution gamma;
	
	public void setUp(){
		gamma = new GammaDistribution(1.0,1.0);
	}
	
	
	public void testPdf(){
		double[] draws = {0.03940330010825375,
		2.627225067228779E-7,1.0041101359127637E-12,
		4.91785903279774E-26,3.514490012590808E-4,
		2.6809177078691704,0.6052676518349311,
		2.302603738832266,0.028437159266857737,
		9.758788870907807E-6,0.004044877559857486,
		0.46881898702825836,6.122578454481994E-4,
		0.032807341953430144,5.549459418961543E-12,
		0.015969123047838672,0.04142364322007368,
		1.987143358189955E-14,0.02048426563227116,
		4.411296859532164E-6,1.0783156542824202E-9,
		0.02471823254442586,0.0013085310262056567,
		7.920097366245217E-6,0.0028807555834354424,
		0.00662306201840817,2.471277083954884E-4,
		5.806921850707865E-12,0.017400249521747593,
		0.8963306540837759,4.7819720630466005E-6,
		1.0512415913015512E-13,0.015087162700483256,
		0.09281534204841962,0.1390844572662422,
		0.07967474963651275,3.5198284619969146E-4,
		2.2330630785534887E-5,2.876641888385043E-7,
		0.19699258423017366,2.9561059751726186,
		4.105783359090399E-23,7.522994144221164E-4,
		1.8182699732902104E-5,45.92272794557283,
		18.33871885427538,3.9057408623567763E-7,
		0.25538394651934115,0.02913906994205684,
		1.8267019324555747,14.806412605878938,
		6.803127907025164E-7,8.958777098961821E-7,
		0.5413594475794531,5.380534039427581E-5,
		3.44974581941716,0.4935768536146689,
		0.003029091772877977,1.79333556779526,
		0.011464018663154343,2.0501487081231042,
		3.055455129258458E-6,0.17213467572390884,
		12.763846002447147,1.7560872521841857E-15,
		0.7442992705606115,1.3357289734884068,
		0.22410022585668543,0.011306054502419505,
		20.70024303174516,5.531460430927817E-4,
		1.460389617093193E-6,3.469294145499206E-8,
		0.004718084496269312,2.2412785100466265E-7,
		1.401670236363704E-7,1.4977967920499572E-14,
		0.5752779698484698,3.027035645091465E-12,
		0.011396530466122705,1.0927116976905502,
		1.0363496699666522E-5,0.45189054652813954,
		0.0012616984908915352,2.8241046053732752,
		4.572022164165971E-5,12.270008142363398,
		4.103673378755346E-14,2.4100257419230736E-7,
		0.2593714784569619,0.012624431868225393,
		2.2219902965829735E-4,0.06366369696551914,
		0.15449211570942137,0.010270142074288956,
		2.266546989858456E-9,2.470609240907427,
		21.153838510468695,8.011188097589497E-9,
		7.598441347390439E-8};
		
		System.out.println("Testing " + draws.length + " gamma draws");
		
		for(int i = 0; i < draws.length; i++){
			double j = i/500.0 + 1.0;
			
			double shape = 1.0/j;
			double scale = j;
				
			gamma.setShape(shape);
			gamma.setScale(scale);
			
			double value = draws[i];
		
			
			double mypdf = Math.pow(value, shape-1)/GammaFunction.gamma(shape)
					*Math.exp(-value/scale)/Math.pow(scale, shape);
			
			assertEquals(mypdf,gamma.pdf(value),1e-10);
		}
		gamma.setScale(1.0);
		gamma.setShape(1.0);

	}
	
	
}
