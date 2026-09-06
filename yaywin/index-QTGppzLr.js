import{a2 as b,b as g,k as h,r as B,B as M,A as P,aT as x,_ as C,N as m,F as o,H as a,O as R,a4 as $,Q as e,R as i,af as y,ag as f}from"./index-BywI3krw.js";const w=b({__name:"index",setup(k,{expose:s}){s();const{t:l}=g(),t=h(),r=B([{title:l("betAmounts"),body:[]},{title:l("rewordPercent"),body:[]}]),d=async()=>{const c=await P(x());c&&c.data.map(p=>(r[0].body.push(p.lotteryAmount+""),r[1].body.push(p.exchange_Rate*1e3*100/1e3+"%"),p))};M(()=>{d()});function _(){t.back()}const n={$t:l,router:t,pointRule:r,getProductRules:d,onClick:_,toBet:()=>{sessionStorage.setItem("clickedGameType","lottery"),t.push({path:"/"})}};return Object.defineProperty(n,"__isScriptSetup",{enumerable:!1,value:!0}),n}}),A={class:"pointMall-rule__container content"},N={class:"pointMall-rule__container-pointRule"},S={class:"pointMall-rule__container-pointRule__title"},F={class:"pointMall-rule__container-pointRule__body"},I={class:"toBet"};function V(k,s,l,t,r,d){const _=m("NavBar"),v=m("van-icon");return o(),a("div",A,[R(_,{title:t.$t("pointsRule"),"left-arrow":"",onClickLeft:t.onClick},null,8,["title"]),$(` <div class="pointMall-rule__container-claimRule">
			<div class="pointMall-rule__container-claimRule__title">1.{{ $t('claimPoints') }}</div>
			<div class="pointMall-rule__container-claimRule__body">
				<div>{{ $t('descRules1') }}</div>
				<div>
					<p>{{ $t('inviteFriends') }}</p>
					<p>{{ $t('earnPoints') }}</p>
				</div>
				<div @click="router.push({ path: '/main/InvitationBonus' })">
					<span> {{ $t('toClaim') }} </span>
					<van-icon name="upgrade" />
				</div>
			</div>
		</div> `),e("div",N,[e("div",S,i(t.$t("bonusPoints")),1),e("div",F,[e("div",null,i(t.$t("descRules2")),1),e("div",null,[(o(!0),a(y,null,f(t.pointRule,(n,c)=>(o(),a("div",{key:c},[e("p",null,i(n.title),1),(o(!0),a(y,null,f(n.body,u=>(o(),a("li",{key:u},i(u),1))),128))]))),128))]),e("div",{onClick:s[0]||(s[0]=n=>t.toBet())},[e("span",I,i(t.$t("goBetting")),1),R(v,{name:"upgrade",color:"#D23838"})])])])])}const D=C(w,[["render",V],["__scopeId","data-v-26d63714"],["__file","/home/jenkins/agent/workspace/AR092-Pages-pakistan-yaywin/src/views/activity/PointMall/Rules/index.vue"]]);export{D as default};
