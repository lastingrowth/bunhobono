<template>
  <main class="stats-dashboard">
    <div class="refresh-toolbar">
      <span><i></i>마지막 갱신 {{ updatedText }}</span>
      <button type="button" :disabled="loading" @click="refresh">
        {{ loading ? '갱신 중' : '새로고침' }}
      </button>
    </div>
    <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

    <section class="summary-grid">
      <article v-for="item in summaries" :key="item.label" :class="item.color"><b>{{ item.icon }}</b><div><span>{{ item.label }}</span><strong>{{ item.value }}<small>{{ item.unit }}</small></strong><p>{{ item.note }}</p></div></article>
    </section>

    <section class="primary-grid">
      <article class="panel chart-panel">
        <PanelTitle :title="trafficTitle">
          <div class="traffic-tools">
            <div class="legend"><span><i class="resident"></i>입주민</span><span><i class="visitor"></i>비입주민</span></div>
            <div class="period-switch" aria-label="입차 추이 조회 단위">
              <button v-for="period in periods" :key="period.value" type="button" :class="{ active: stats.entryPeriod === period.value }" @click="stats.changeEntryPeriod(period.value)">{{ period.label }}</button>
            </div>
          </div>
        </PanelTitle>
        <div class="bar-chart"><div v-for="item in stats.entryCompareStats" :key="item.label" class="bar-item"><div><i class="resident" :style="barHeight(item.residentCount)"><b>{{ item.residentCount }}</b></i><i class="visitor" :style="barHeight(item.nonResidentCount)"><b>{{ item.nonResidentCount }}</b></i></div><span>{{ item.label }}</span></div></div>
      </article>

      <article class="panel mix-panel">
        <PanelTitle title="현재 주차 현황" />
        <div class="mix-body"><div class="donut" :style="donutStyle"><div><span>현재 주차</span><strong>{{ stats.currentParkingTotal }}</strong><small>대</small></div></div><div class="mix-list"><div v-for="item in stats.currentParkingTypeStats" :key="item.key"><i :class="item.key"></i><span>{{ item.label }}</span><strong>{{ item.count }}대</strong><small>{{ item.percent }}%</small></div></div></div>
      </article>
    </section>

    <section class="secondary-grid">
      <article class="panel capacity-panel">
        <PanelTitle title="주차 사용률"><button @click="router.push('/admin/parking-map')">배치도 보기 →</button></PanelTitle>
        <div class="floors">
          <div v-for="floor in floors" :key="floor.parkingNo" class="floor">
            <div class="floor-bar-heading"><div><h3>{{ floorName(floor) }}</h3><em :class="statusClass(floor.percent)">{{ statusText(floor.percent) }}</em></div><strong>{{ floor.percent }}<small>%</small></strong></div>
            <div class="floor-bar-track" role="img" :aria-label="`${floorName(floor)} 사용률 ${floor.percent}%`"><i :style="{ width: `${floor.percent}%` }"></i></div>
            <dl><div><dt>사용 중</dt><dd>{{ floor.used }}면</dd></div><div><dt>잔여</dt><dd>{{ floor.available }}면</dd></div><div><dt>전체</dt><dd>{{ floor.total }}면</dd></div></dl>
          </div>
          <p v-if="!floors.length" class="empty">B1·B2 주차장 데이터가 없습니다.</p>
        </div>
      </article>

      <article class="panel billing-panel">
        <PanelTitle title="오늘 정산 현황"><button @click="router.push('/admin/billing')">정산 관리 →</button></PanelTitle>
        <div class="revenue"><span>결제 완료 금액</span><strong>{{ won(todayRevenue) }}</strong></div>
        <div class="bill-counts"><div><i class="paid"></i><span>결제 완료</span><strong>{{ todayPaid.length }}건</strong></div><div><i class="unpaid"></i><span>미결제</span><strong>{{ unpaidCount }}건</strong></div></div>
        <div class="progress"><i :style="{width:`${paidRate}%`}"></i></div><p>현재 목록 기준 정산 완료율 <strong>{{ paidRate }}%</strong></p>
      </article>

      <article class="panel insight-panel">
        <PanelTitle title="오늘의 이용 정보" />
        <div class="insights"><div><b>↗</b><p>가장 혼잡한 시간<strong>{{ busiest }}</strong></p></div><div><b>◷</b><p>방문차량 평균 주차<strong>{{ visitAverage }}</strong></p></div><div><b>!</b><p>장기주차·만료 알림<strong>{{ warningCount }}건</strong></p></div></div>
      </article>
    </section>
  </main>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStatisticsStore } from './statisticsStore'
import { useBillingStore } from '@/features/billing/billingStore'

const PanelTitle=defineComponent({props:{title:String},setup(p,{slots}){return()=>h('header',{class:'panel-title'},[h('h2',p.title),slots.default?.()])}})
const router=useRouter(), stats=useStatisticsStore(), billing=useBillingStore(), updatedAt=ref(null); let timer
const loading=computed(()=>stats.loading||billing.loading), errorMessage=computed(()=>stats.errorMessage||billing.errorMessage)
const floors=computed(()=>stats.parkingUsageStats.filter(x=>/B1|B2/i.test(`${x.parkingName||''}`)))
const totalSpaces=computed(()=>floors.value.reduce((s,x)=>s+x.total,0)), available=computed(()=>floors.value.reduce((s,x)=>s+x.available,0))
const warningCount=computed(()=>stats.nonResidentWarningStats.reduce((s,x)=>s+Number(x.count||0),0))
const isToday=v=>{if(!v)return false;const d=new Date(v);return !Number.isNaN(d.getTime())&&d.toDateString()===new Date().toDateString()}
const todayPaid=computed(()=>billing.adminBillingList.filter(x=>x.billStatus==='PAID'&&isToday(x.paidAt)))
const todayRevenue=computed(()=>todayPaid.value.reduce((s,x)=>s+Number(x.billAmount||0),0))
const unpaidCount=computed(()=>billing.adminBillingList.filter(x=>!x.billStatus||x.billStatus==='UNPAID').length)
const paidRate=computed(()=>{const all=billing.adminBillingList.length,paid=billing.adminBillingList.filter(x=>x.billStatus==='PAID').length;return all?Math.round(paid/all*100):0})
const summaries=computed(()=>[
  {label:'현재 주차',value:stats.currentParkingTotal,unit:'대',note:`전체 ${totalSpaces.value}면 기준`,icon:'차',color:'blue'},
  {label:'잔여 주차면',value:available.value,unit:'면',note:available.value<=10?'혼잡에 대비하세요':'현재 원활합니다',icon:'면',color:'green'},
  {label:'오늘 입차',value:stats.todayInCount,unit:'대',note:`출차 ${stats.todayOutCount}대`,icon:'입',color:'purple'},
  {label:'오늘 정산',value:todayPaid.value.length,unit:'건',note:won(todayRevenue.value),icon:'₩',color:'yellow'},
  {label:'확인 필요',value:warningCount.value+unpaidCount.value,unit:'건',note:`미결제 ${unpaidCount.value}건 포함`,icon:'!',color:'red'}])
const busiest=computed(()=>{const x=[...stats.hourlyParkingStats].sort((a,b)=>b.count-a.count)[0];return x?.count?`${x.label} · ${x.count}대`:'데이터 없음'})
const visitAverage=computed(()=>stats.averageParkingTimeStats.find(x=>x.key==='visit')?.text||'데이터 없음')
const updatedText=computed(()=>updatedAt.value?updatedAt.value.toLocaleTimeString('ko-KR',{hour:'2-digit',minute:'2-digit'}):'조회 중')
const periods=[{label:'일간',value:'weekly'},{label:'주간',value:'monthly'},{label:'월간',value:'yearly'}]
const trafficTitle=computed(()=>{
  if(stats.entryPeriod==='monthly') return `${stats.entryPeriodLabel} 주간 입차 추이`
  if(stats.entryPeriod==='yearly') return `${stats.entryPeriodLabel} 월간 입차 추이`
  return '최근 7일 입차 추이'
})
const donutStyle=computed(()=>{const r=stats.currentParkingTypeStats.find(x=>x.key==='resident')?.percent||0,v=stats.currentParkingTypeStats.find(x=>x.key==='visit')?.percent||0;return{background:`conic-gradient(#ffc928 0 ${r}%,#78bd91 ${r}% ${r+v}%,#ef969c ${r+v}% 100%)`}})
const floorName=x=>`${x.parkingName}`.match(/B[12]/i)?.[0]?.toUpperCase()||x.parkingName
const statusText=p=>p>=85?'혼잡':p>=65?'보통':'여유', statusClass=p=>p>=85?'busy':p>=65?'normal':'free'
const barHeight=n=>({height:`${Math.max(n/stats.entryCompareMaxCount*100,n?8:2)}%`}), won=n=>`${Number(n||0).toLocaleString('ko-KR')}원`
const refresh=async()=>{await Promise.all([stats.loadStatistics({silent:!!updatedAt.value}),billing.loadAdminBillingList(false)]);updatedAt.value=new Date()}
onMounted(async()=>{stats.changeEntryPeriod('weekly');await refresh();timer=window.setInterval(refresh,60000)});onUnmounted(()=>clearInterval(timer))
</script>

<style scoped>
.stats-dashboard{min-height:100%;padding:30px;background:#f4f7fb;color:#172033}.stats-header{display:flex;align-items:flex-end;justify-content:space-between;margin-bottom:24px}.stats-header>div:first-child>span,.panel-title span{color:#3978f6;font-size:11px;font-weight:900;letter-spacing:.15em}.stats-header h1{margin:5px 0 6px;font-size:30px;letter-spacing:-.04em}.stats-header p{margin:0;color:#7d8798}.updated{display:flex;align-items:center;gap:11px;padding:10px 12px 10px 16px;border:1px solid #e1e7f0;border-radius:14px;background:white}.updated>i{width:8px;height:8px;border-radius:50%;background:#20b486;box-shadow:0 0 0 5px #20b4861c}.updated p{display:flex;flex-direction:column;font-size:10px}.updated strong{color:#172033;font-size:13px}.updated button,.panel-title button{padding:9px 12px;border:0;border-radius:9px;background:#eef3fb;color:#345889;font-weight:800;cursor:pointer}.error-message{padding:12px;border-radius:10px;background:#fff0ed;color:#bf4d35}.summary-grid{display:grid;grid-template-columns:repeat(5,1fr);gap:14px;margin-bottom:16px}.summary-grid article{display:flex;gap:13px;padding:19px;border:1px solid #e3e9f2;border-radius:18px;background:white;box-shadow:0 8px 25px #23375a0d}.summary-grid article>b{display:grid;place-items:center;width:42px;height:42px;border-radius:13px;font-size:12px}.summary-grid .blue>b{background:#eaf2ff;color:#3978f6}.summary-grid .green>b{background:#e4f8f1;color:#159973}.summary-grid .purple>b{background:#f0eaff;color:#7957d5}.summary-grid .yellow>b{background:#fff4d8;color:#bd7c10}.summary-grid .red>b{background:#ffede8;color:#d35d43}.summary-grid span{display:block;color:#7d8798;font-size:12px}.summary-grid strong{display:block;margin:3px 0;font-size:25px}.summary-grid small{margin-left:3px;font-size:13px}.summary-grid p{margin:0;color:#9ba4b2;font-size:11px}.primary-grid{display:grid;grid-template-columns:1.55fr .75fr;gap:16px;margin-bottom:16px}.secondary-grid{display:grid;grid-template-columns:1.3fr .75fr .65fr;gap:16px}.panel{padding:22px;border:1px solid #e3e9f2;border-radius:20px;background:white;box-shadow:0 9px 28px #23375a0d}.panel-title{display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:20px}.panel-title h2{margin:4px 0 0;font-size:18px}.floors{display:grid;grid-template-columns:1fr 1fr;gap:16px}.floor{display:flex;align-items:center;gap:18px;padding:18px;border-radius:16px;background:#f8faff}.ring{display:grid;place-items:center;flex:0 0 96px;height:96px;border-radius:50%}.ring>div{display:flex;align-items:baseline;justify-content:center;width:72px;height:72px;border-radius:50%;background:white}.ring strong{font-size:25px}.floor-info{flex:1}.floor-info>div{display:flex;align-items:center;justify-content:space-between}.floor-info h3{margin:0;font-size:22px}.floor-info em{padding:4px 8px;border-radius:20px;font-size:10px;font-style:normal;font-weight:900}.floor-info em.free{background:#e5f7f1;color:#168b6a}.floor-info em.normal{background:#fff4dc;color:#ae7210}.floor-info em.busy{background:#ffebe7;color:#c4533b}.track{display:block;height:7px;margin:12px 0;background:#e7ecf3;border-radius:9px;overflow:hidden}.track i{display:block;height:100%;background:linear-gradient(90deg,#3978f6,#79a7ff)}dl{display:flex;gap:15px;margin:0}dl div{display:flex;flex-direction:column}dt{color:#969faf;font-size:10px}dd{margin:2px 0;font-size:12px;font-weight:800}.mix-body{display:flex;align-items:center;gap:25px;padding:7px 0}.donut{display:grid;place-items:center;flex:0 0 145px;height:145px;border-radius:50%}.donut>div{display:flex;align-items:baseline;justify-content:center;flex-wrap:wrap;width:100px;height:100px;border-radius:50%;background:white}.donut span{align-self:flex-end;width:100%;text-align:center;color:#8a94a5;font-size:11px}.donut strong{font-size:30px}.mix-list{flex:1}.mix-list>div{display:grid;grid-template-columns:9px 1fr auto auto;gap:7px;align-items:center;padding:9px 0;border-bottom:1px solid #edf0f5;font-size:12px}.mix-list i,.legend i,.bill-counts i{width:8px;height:8px;border-radius:50%}.mix-list .resident,.legend .resident{background:#3978f6}.mix-list .visit,.legend .visitor{background:#20b486}.mix-list .unknown{background:#ff8765}.mix-list small{color:#919bab}.legend{display:flex;gap:12px;color:#778294;font-size:11px}.legend span{display:flex;align-items:center;gap:5px}.bar-chart{display:flex;height:210px;gap:12px;padding:15px 8px 0;border-bottom:1px solid #dfe5ee;background:repeating-linear-gradient(to bottom,#fff 0,#fff 48px,#edf1f6 49px)}.bar-item{display:flex;flex:1;flex-direction:column;justify-content:flex-end;text-align:center}.bar-item>div{display:flex;align-items:flex-end;justify-content:center;gap:4px;height:170px}.bar-item i{position:relative;width:17px;min-height:2px;border-radius:5px 5px 2px 2px}.bar-item i.resident{background:linear-gradient(#6d9bf8,#3978f6)}.bar-item i.visitor{background:linear-gradient(#4bc7a1,#20b486)}.bar-item b{position:absolute;top:-16px;left:50%;transform:translateX(-50%);font-size:9px}.bar-item>span{margin:8px 0;color:#858fa0;font-size:10px}.revenue{padding:18px;border-radius:15px;background:linear-gradient(135deg,#1d2a44,#354c75);color:white}.revenue span{color:#bdc9da;font-size:11px}.revenue strong{display:block;margin-top:6px;font-size:25px}.bill-counts{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin:16px 0}.bill-counts div{display:grid;grid-template-columns:8px 1fr;gap:4px 7px;padding:10px;background:#f7f9fc;border-radius:10px}.bill-counts .paid{background:#20b486}.bill-counts .unpaid{background:#ff8765}.bill-counts span{font-size:11px}.bill-counts strong{grid-column:2}.progress{height:7px;border-radius:8px;background:#edf1f5;overflow:hidden}.progress i{display:block;height:100%;background:#20b486}.billing-panel>p{color:#8791a2;font-size:11px}.insights>div{display:flex;align-items:center;gap:10px;padding:12px 0;border-bottom:1px solid #edf0f5}.insights b{display:grid;place-items:center;width:30px;height:30px;border-radius:9px;background:#edf3ff;color:#3978f6}.insights p{display:flex;flex-direction:column;margin:0;color:#818b9c;font-size:10px}.insights strong{margin-top:3px;color:#26334a;font-size:13px}.empty{grid-column:1/-1;text-align:center;color:#8d97a8}@media(max-width:1200px){.summary-grid{grid-template-columns:repeat(3,1fr)}.secondary-grid{grid-template-columns:1fr 1fr}.insight-panel{grid-column:1/-1}}@media(max-width:850px){.stats-dashboard{padding:18px}.primary-grid,.secondary-grid{grid-template-columns:1fr}.floors{grid-template-columns:1fr}.insight-panel{grid-column:auto}}@media(max-width:600px){.stats-header{align-items:flex-start;flex-direction:column;gap:16px}.summary-grid{grid-template-columns:1fr}.updated{width:100%;box-sizing:border-box}.floor,.mix-body{flex-direction:column}.mix-list{width:100%}}
/* 관리자 주차장 화면과 동일한 다크 관제 테마 */
.stats-dashboard{background:#24292e;color:#eef1f3}.stats-header>div:first-child>span,.panel-title span{color:#ffc928}.stats-header h1,.panel-title h2{color:#f4f6f7}.stats-header p{color:#9da6ad}.updated{border-color:#505960;background:#2b3035}.updated p{color:#9da6ad}.updated strong{color:#f4f6f7}.updated button,.panel-title button{border:1px solid #69737b;background:#343a40;color:#eef1f3}.updated button:hover,.panel-title button:hover{border-color:#ffc928;color:#ffc928;background:#3a4147}.error-message{border:1px solid #79484d;background:#4c3438;color:#f2a4aa}.summary-grid article,.panel{border-color:#505960;background:#2b3035;box-shadow:0 10px 28px rgba(0,0,0,.18)}.summary-grid article>b{border:1px solid #596168;background:#343a40!important;color:#dce1e4!important}.summary-grid span,.summary-grid p{color:#9da6ad}.summary-grid strong{color:#f5f6f7}.floor{border:1px solid #454e55;background:#30363b}.ring>div{background:#2b3035}.ring strong,.ring small,.floor-info h3,dd{color:#eef1f3}.floor-info em.free{background:#354c3d;color:#8fc7a6}.floor-info em.normal{background:#554a31;color:#f0d36f}.floor-info em.busy{background:#603c41;color:#f0a1a6}.track{background:#20262b}.track i{background:linear-gradient(90deg,#9b7b24,#ffc928)}dt{color:#8e989f}.donut>div{background:#2b3035}.donut span,.mix-list small{color:#929ca3}.donut strong,.donut small,.mix-list strong,.mix-list span{color:#eef1f3}.mix-list>div,.insights>div{border-color:#485158}.mix-list .resident,.legend .resident{background:#ffc928}.mix-list .visit,.legend .visitor{background:#78bd91}.mix-list .unknown{background:#ef969c}.legend{color:#aeb6bc}.bar-chart{border-color:#596168;background:repeating-linear-gradient(to bottom,#2b3035 0,#2b3035 48px,#41484e 49px)}.bar-item i.resident{background:linear-gradient(#ffe177,#ffc928)}.bar-item i.visitor{background:linear-gradient(#9bd4ae,#5ca778)}.bar-item b{color:#eef1f3}.bar-item>span{color:#aeb6bc}.revenue{border:1px solid #596168;background:linear-gradient(135deg,#20252a,#3a4147)}.revenue span{color:#aeb6bc}.bill-counts div{border:1px solid #485158;background:#30363b}.bill-counts span,.bill-counts strong{color:#eef1f3}.progress{background:#20262b}.progress i{background:#78bd91}.billing-panel>p{color:#9da6ad}.billing-panel>p strong{color:#eef1f3}.insights b{border:1px solid #665b36;background:#3b3b32;color:#ffc928}.insights p{color:#9da6ad}.insights strong{color:#eef1f3}.empty{color:#9da6ad}
/* 층별 현황은 사각 카드·가로 막대 대신 독립된 원형 계기판으로 표현한다. */
.capacity-panel .floors{gap:0;min-height:230px}.capacity-panel .floor{position:relative;display:grid;grid-template-columns:1fr 150px 1fr;align-items:center;gap:20px;padding:4px 28px;border:0;border-radius:0;background:transparent}.capacity-panel .floor+ .floor{border-left:1px solid #505960}.floor-name{text-align:right}.floor-name h3{margin:0 0 8px;color:#f5f6f7;font-size:30px}.floor-name em{display:inline-block;padding:5px 10px;border-radius:20px;font-size:11px;font-style:normal;font-weight:900}.floor-name em.free{background:#354c3d;color:#8fc7a6}.floor-name em.normal{background:#554a31;color:#f0d36f}.floor-name em.busy{background:#603c41;color:#f0a1a6}.capacity-panel .ring{width:150px;height:150px;box-shadow:0 0 0 1px #3e464c,0 12px 25px rgba(0,0,0,.22)}.capacity-panel .ring>div{display:flex;flex-direction:column;align-items:center;width:112px;height:112px}.capacity-panel .ring strong{margin-top:25px;font-size:34px}.capacity-panel .ring small{margin-top:0;color:#9da6ad;font-size:10px}.capacity-panel dl{display:grid;gap:8px}.capacity-panel dl div{display:grid;grid-template-columns:52px auto;align-items:baseline}.capacity-panel dt{font-size:10px}.capacity-panel dd{font-size:14px}@media(max-width:1350px){.capacity-panel .floor{grid-template-columns:1fr;justify-items:center;text-align:center}.floor-name{text-align:center}.capacity-panel dl{grid-template-columns:repeat(3,1fr)}.capacity-panel dl div{display:flex;align-items:center;flex-direction:column}.capacity-panel .floor+ .floor{border-left:1px solid #505960}}@media(max-width:650px){.capacity-panel .floors{grid-template-columns:1fr}.capacity-panel .floor{padding:20px}.capacity-panel .floor+ .floor{border-top:1px solid #505960;border-left:0}}
/* 두 번째 줄: 차량 구성은 왼쪽, 최근 입차 추이는 오른쪽 */
.primary-grid{grid-template-columns:.75fr 1.55fr}.mix-panel{grid-column:1;grid-row:1}.chart-panel{grid-column:2;grid-row:1}.capacity-panel{grid-column:1;grid-row:1}
@media(max-width:850px){.mix-panel,.chart-panel,.capacity-panel{grid-column:auto;grid-row:auto}}
/* B1과 B2를 두 줄짜리 가로 사용률 그래프로 표시한다. */
.capacity-panel .floors{display:grid;grid-template-columns:1fr;gap:15px;min-height:0}.capacity-panel .floor{display:block;padding:18px 20px;border:1px solid #485158!important;border-radius:12px;background:#30363b}.capacity-panel .floor+.floor{border-left:1px solid #485158}.floor-bar-heading{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}.floor-bar-heading>div{display:flex;align-items:center;gap:10px}.floor-bar-heading h3{margin:0;color:#f5f6f7;font-size:21px}.floor-bar-heading em{padding:4px 9px;border-radius:20px;font-size:10px;font-style:normal;font-weight:900}.floor-bar-heading em.free{background:#354c3d;color:#8fc7a6}.floor-bar-heading em.normal{background:#554a31;color:#f0d36f}.floor-bar-heading em.busy{background:#603c41;color:#f0a1a6}.floor-bar-heading>strong{color:#ffc928;font-size:24px}.floor-bar-heading>strong small{margin-left:2px;color:#c7cdd1;font-size:12px}.floor-bar-track{height:17px;padding:3px;border:1px solid #505960;border-radius:12px;background:#20262b;overflow:hidden}.floor-bar-track i{display:block;height:100%;border-radius:8px;background:linear-gradient(90deg,#a88522 0%,#ffc928 75%,#ffe078 100%);box-shadow:0 0 10px rgba(255,201,40,.22);transition:width .35s ease}.capacity-panel dl{display:flex;justify-content:flex-end;gap:24px;margin-top:11px}.capacity-panel dl div{display:flex;align-items:baseline;flex-direction:row;gap:5px}.capacity-panel dt{font-size:10px}.capacity-panel dd{font-size:12px}@media(max-width:650px){.capacity-panel .floor{padding:15px}.capacity-panel dl{justify-content:space-between;gap:8px}.capacity-panel dl div{align-items:flex-start;flex-direction:column;gap:1px}}
/* 상단 소개 영역을 제거한 압축형 대시보드 */
.stats-dashboard{padding:18px 24px}.summary-grid{gap:10px;margin-bottom:10px}.summary-grid article{padding:13px 15px}.summary-grid article>b{width:36px;height:36px}.summary-grid strong{font-size:21px}.primary-grid{gap:10px;margin-bottom:10px}.secondary-grid{gap:10px}.panel{padding:16px}.panel-title{margin-bottom:12px}.panel-title h2{font-size:16px}.capacity-panel .floors{gap:9px}.capacity-panel .floor{padding:11px 15px}.floor-bar-heading{margin-bottom:7px}.floor-bar-heading h3{font-size:18px}.floor-bar-heading>strong{font-size:20px}.floor-bar-track{height:13px}.capacity-panel dl{margin-top:7px}.mix-body{padding:0}.donut{flex-basis:120px;height:120px}.donut>div{width:84px;height:84px}.donut strong{font-size:25px}.mix-list>div{padding:6px 0}.bar-chart{height:172px;padding-top:8px}.bar-item>div{height:137px}.revenue{padding:13px}.revenue strong{font-size:21px}.bill-counts{margin:10px 0}.bill-counts div{padding:7px}.insights>div{padding:8px 0}@media(max-width:850px){.stats-dashboard{padding:14px}}
.refresh-toolbar{display:flex;align-items:center;justify-content:flex-end;gap:10px;height:28px;margin-bottom:7px;color:#9da6ad;font-size:10px}.refresh-toolbar span{display:flex;align-items:center;gap:6px}.refresh-toolbar span i{width:6px;height:6px;border-radius:50%;background:#78bd91;box-shadow:0 0 0 3px rgba(120,189,145,.12)}.refresh-toolbar button{padding:5px 10px;border:1px solid #596168;border-radius:6px;color:#e7eaec;background:#343a40;font-size:10px;font-weight:800;cursor:pointer}.refresh-toolbar button:hover{border-color:#ffc928;color:#ffc928}.refresh-toolbar button:disabled{opacity:.55;cursor:wait}
.traffic-tools{display:flex;align-items:center;gap:12px}.period-switch{display:flex;gap:2px}.period-switch button{min-width:40px;padding:4px 7px;border:0;border-radius:5px;color:#9da6ad;background:transparent;font-size:10px;font-weight:800;cursor:pointer}.period-switch button:hover{color:#eef1f3}.period-switch button.active{color:#20252a;background:#ffc928}@media(max-width:600px){.chart-panel .panel-title{align-items:flex-start;flex-direction:column;gap:9px}.traffic-tools{width:100%;justify-content:space-between}}
.capacity-panel{padding:16px;border:1px solid #505960;background:#2b3035;box-shadow:0 10px 28px rgba(0,0,0,.18)}.capacity-panel>.panel-title{padding:0}.capacity-panel .floor{padding:10px 4px;border:0!important;border-radius:0;background:transparent}.capacity-panel .floor+.floor{padding-top:16px;border-top:1px solid #485158!important;border-left:0!important}

/* 패널 제목은 내용보다 먼저 튀지 않도록 저채도 회색으로 통일한다. */
.stats-dashboard .panel-title h2 {
  color: #c3c9ce;
  font-weight: 750;
  letter-spacing: -.025em;
  text-shadow: none;
}

/* 통계 화면의 최외곽 영역과 갱신 도구줄을 관리자 헤더 바로 아래에 붙인다. */
:global(.admin-layout.admin-statistics-layout .content) {
  padding-top: 0 !important;
  background: #24292e !important;
}

.stats-dashboard {
  padding-top: 0;
}

/* 관리자 메뉴 최상단 통계 대시보드: 1280 × 720 한 화면 배치 */
@media (min-width: 1101px) and (max-width: 1400px) and (max-height: 800px) {
  .stats-dashboard {
    box-sizing: border-box;
    height: calc(100dvh - var(--header-height));
    min-height: 0;
    display: grid;
    grid-template-rows: 22px minmax(64px, auto) minmax(0, 1.05fr) minmax(0, .95fr);
    gap: 7px;
    padding: 8px 12px;
    overflow: hidden;
  }

  .refresh-toolbar {
    height: 22px;
    margin: 0;
  }

  .refresh-toolbar button {
    padding: 3px 8px;
  }

  .error-message {
    position: absolute;
    z-index: 5;
    top: 34px;
    right: 12px;
    left: 12px;
    margin: 0;
    padding: 7px 10px;
    font-size: 10px;
  }

  .summary-grid {
    min-height: 64px;
    gap: 7px;
    margin: 0;
  }

  .summary-grid article {
    min-width: 0;
    gap: 8px;
    padding: 8px 9px;
    border-radius: 10px;
  }

  .summary-grid article > b {
    width: 29px;
    height: 29px;
    flex: 0 0 29px;
    border-radius: 8px;
    font-size: 10px;
  }

  .summary-grid span { font-size: 10px; }
  .summary-grid strong { margin: 1px 0; font-size: 18px; line-height: 1.05; }
  .summary-grid small { font-size: 10px; }
  .summary-grid p { overflow: hidden; font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }

  .primary-grid,
  .secondary-grid {
    min-height: 0;
    gap: 7px;
    margin: 0;
  }

  .primary-grid {
    grid-template-columns: minmax(250px, .72fr) minmax(0, 1.55fr);
  }

  .secondary-grid {
    grid-template-columns: minmax(0, 1.3fr) minmax(210px, .75fr) minmax(190px, .65fr);
  }

  .panel {
    min-width: 0;
    min-height: 0;
    padding: 9px 11px;
    border-radius: 11px;
    overflow: hidden;
  }

  .panel-title {
    min-height: 23px;
    margin-bottom: 5px;
  }

  .panel-title h2 {
    margin-top: 1px;
    font-size: 13px;
  }

  .panel-title button {
    padding: 4px 7px;
    border-radius: 5px;
    font-size: 9px;
  }

  .mix-body {
    height: calc(100% - 28px);
    gap: 13px;
  }

  .donut {
    flex-basis: 92px;
    height: 92px;
  }

  .donut > div {
    width: 64px;
    height: 64px;
  }

  .donut span { font-size: 9px; }
  .donut strong { font-size: 20px; }
  .mix-list > div { padding: 3px 0; font-size: 10px; }

  .capacity-panel {
    padding: 9px 11px;
  }

  .capacity-panel .floors {
    height: calc(100% - 28px);
    gap: 4px;
  }

  .capacity-panel .floor,
  .capacity-panel .floor + .floor {
    box-sizing: border-box;
    padding: 5px 4px;
  }

  .floor-bar-heading { margin-bottom: 3px; }
  .floor-bar-heading h3 { font-size: 14px; }
  .floor-bar-heading em { padding: 2px 6px; font-size: 8px; }
  .floor-bar-heading > strong { font-size: 16px; }
  .floor-bar-heading > strong small { font-size: 9px; }
  .floor-bar-track { height: 9px; padding: 2px; }
  .capacity-panel dl { gap: 14px; margin-top: 3px; }
  .capacity-panel dt { font-size: 8px; }
  .capacity-panel dd { font-size: 10px; }

  .traffic-tools { gap: 6px; }
  .legend { gap: 6px; font-size: 8px; }
  .period-switch button { min-width: 31px; padding: 2px 4px; font-size: 8px; }
  .bar-chart { height: calc(100% - 28px); min-height: 90px; gap: 5px; padding: 5px 3px 0; }
  .bar-item > div { height: calc(100% - 19px); min-height: 62px; }
  .bar-item i { width: 12px; }
  .bar-item > span { margin: 4px 0; font-size: 8px; }

  .revenue { padding: 7px 9px; border-radius: 8px; }
  .revenue span { font-size: 9px; }
  .revenue strong { margin-top: 2px; font-size: 17px; }
  .bill-counts { gap: 5px; margin: 6px 0; }
  .bill-counts div { padding: 5px; border-radius: 6px; }
  .bill-counts span, .bill-counts strong { font-size: 9px; }
  .billing-panel > p { margin: 5px 0 0; font-size: 9px; }

  .insights > div { gap: 7px; padding: 5px 0; }
  .insights b { width: 24px; height: 24px; border-radius: 6px; }
  .insights p { font-size: 8px; }
  .insights strong { margin-top: 1px; font-size: 10px; }

  /* 층별 사용률은 패널 폭을 채우는 독립 카드로 표시한다. */
  .capacity-panel .floors {
    height: auto;
    grid-template-rows: repeat(2, auto);
    align-content: start;
    gap: 6px;
  }

  .capacity-panel .floor,
  .capacity-panel .floor + .floor {
    min-height: 0;
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    grid-template-rows: auto auto auto;
    align-content: start;
    justify-items: stretch;
    gap: 3px;
    padding: 6px 9px !important;
    border: 1px solid #485158 !important;
    border-radius: 8px;
    text-align: left;
    background: #30363b;
  }

  .capacity-panel .floor + .floor {
    border-top: 1px solid #485158 !important;
  }

  .capacity-panel .floor-bar-heading {
    width: 100%;
    min-width: 0;
    justify-content: flex-start;
    gap: 7px;
    margin: 0;
    text-align: left;
  }

  .capacity-panel .floor-bar-heading > div {
    gap: 7px;
  }

  .capacity-panel .floor-bar-heading h3 {
    min-width: 25px;
    font-size: 15px;
    letter-spacing: .02em;
  }

  .capacity-panel .floor-bar-heading > strong {
    display: flex;
    align-items: baseline;
    color: #ffc928;
    font-size: 19px;
  }

  .capacity-panel .floor-bar-track {
    width: 100%;
    height: 10px;
    box-sizing: border-box;
  }

  .capacity-panel dl {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 6px;
    margin: 0;
  }

  .capacity-panel dl div {
    min-width: 0;
    display: flex;
    align-items: baseline;
    justify-content: center;
    flex-direction: row;
    gap: 4px;
    padding: 2px 5px;
    border-radius: 4px;
    background: #292f34;
  }

  .capacity-panel dt {
    color: #99a2a9;
    font-size: 8px;
  }

  .capacity-panel dd {
    color: #eef1f3;
    font-size: 10px;
  }
}

.stats-dashboard {
  padding-top: 6px !important;
  border: 0;
  background: #24292e;
  box-shadow: none;
}
</style>
