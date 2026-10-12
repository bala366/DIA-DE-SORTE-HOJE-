package br.com.diadesorte.campeao;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

public class MainActivity extends Activity {
  static final int PICK=1, SAVE=2;
  static final String[] MESES={"JANEIRO","FEVEREIRO","MARÇO","ABRIL","MAIO","JUNHO","JULHO","AGOSTO","SETEMBRO","OUTUBRO","NOVEMBRO","DEZEMBRO"};
  static final int AMARELO=Color.rgb(255,215,0), ESCURO=Color.rgb(35,35,35);
  final ArrayList<Draw> dados=new ArrayList<>();
  final ArrayList<Stat> todas=new ArrayList<>(), candidatas=new ArrayList<>();
  int[] jogo=new int[0]; int mes=0; int retiradasMask=0; int indiceCandidato=0;
  TextView status; LinearLayout raiz; boolean analisado=false;
  static class Draw { int concurso,mask,mes; Draw(int c,int m,int s){concurso=c;mask=m;mes=s;} }
  static class Stat { int tiras,mask,qtd,falhas,atraso,maiorSeq,ultimo; }
  @Override public void onCreate(Bundle b){super.onCreate(b);tela();}
  TextView texto(String t,int tamanho){TextView v=new TextView(this);v.setText(t);v.setTextColor(ESCURO);v.setTextSize(tamanho);v.setPadding(14,12,14,12);return v;}
  void botao(String nome,Runnable acao){Button b=new Button(this);b.setText(nome);b.setAllCaps(false);b.setTextColor(ESCURO);b.setOnClickListener(v->acao.run());raiz.addView(b);}
  void tela(){ScrollView sc=new ScrollView(this);raiz=new LinearLayout(this);raiz.setOrientation(1);raiz.setPadding(14,18,14,22);raiz.setBackgroundColor(Color.WHITE);sc.addView(raiz);setContentView(sc);
    TextView titulo=texto("☀ DIA DE SORTE CAMPEÃO V7",23);titulo.setGravity(Gravity.CENTER);titulo.setTypeface(null,Typeface.BOLD);titulo.setBackgroundColor(AMARELO);raiz.addView(titulo);
    raiz.addView(texto("MOTOR V5 DO WINDOWS • 8 tirinhas • 255 combinações • 7 dezenas + Mês da Sorte",13));
    botao("1. SELECIONAR HISTÓRICO TXT/CSV",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,PICK);});
    botao("2. ANALISAR AS 255 COMBINAÇÕES",this::analisar);
    botao("3. ESCOLHER TIRINHAS PARA FALHA",this::escolherTiras);
    botao("4. GERAR JOGO (MESMO MOTOR V5)",this::gerarJogo);
    botao("5. SALVAR PDF COLORIDO + RANKING",this::salvarPdf);
    status=texto("Selecione o histórico do Dia de Sorte em Downloads.",15);raiz.addView(status);
  }
  void dizer(String s){status.setText(s);}
  @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null)return;try{if(req==PICK)ler(data.getData());else if(req==SAVE)pdf(data.getData());}catch(Exception e){dizer("ERRO: "+e.getMessage());}}
  static int mesNumero(String s){String t=Normalizer.normalize(s.trim().toUpperCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}","");for(int i=0;i<12;i++){String m=Normalizer.normalize(MESES[i],Normalizer.Form.NFD).replaceAll("\\p{M}","");if(t.equals(m))return i+1;}try{int n=Integer.parseInt(t);return n>=1&&n<=12?n:0;}catch(Exception e){return 0;}}
  void ler(Uri uri)throws Exception{
    dados.clear();todas.clear();candidatas.clear();jogo=new int[0];mes=0;retiradasMask=0;analisado=false;
    TreeMap<Integer,Draw> mapa=new TreeMap<>();int rejeitadas=0,linha=0;
    Pattern formato=Pattern.compile("^\\s*(\\d+)\\s*-\\s*((?:\\d{1,2}\\s*,\\s*){6}\\d{1,2})\\s*-\\s*([\\p{L}]+)\\s*$");
    try(BufferedReader br=new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri),StandardCharsets.UTF_8))){String l;
      while((l=br.readLine())!=null){linha++;if(l.trim().isEmpty())continue;Matcher m=formato.matcher(l);if(!m.matches()){rejeitadas++;continue;}
        int concurso=Integer.parseInt(m.group(1));String[] partes=m.group(2).split(",");int mask=0;boolean ok=partes.length==7;
        for(String p:partes){int n=Integer.parseInt(p.trim());if(n<1||n>31||(mask&(1<<(n-1)))!=0)ok=false;else mask|=1<<(n-1);}
        int mm=mesNumero(m.group(3));if(!ok||mm==0||mapa.containsKey(concurso)){rejeitadas++;continue;}mapa.put(concurso,new Draw(concurso,mask,mm));
      }
    }
    if(mapa.size()<10)throw new Exception("Menos de 10 concursos válidos. Formato esperado: 1 - 03,05,08,09,19,21,30 - Fevereiro");
    int primeiro=mapa.firstKey(),ultimo=mapa.lastKey();if(mapa.size()!=ultimo-primeiro+1)throw new Exception("Histórico incompleto: "+mapa.size()+" concursos entre "+primeiro+" e "+ultimo+". Corrija antes de analisar.");
    if(rejeitadas>0)throw new Exception("Foram rejeitadas "+rejeitadas+" linhas. Confira o TXT antes de continuar.");
    dados.addAll(mapa.values());
    dizer("Histórico: "+dados.size()+" concursos ("+primeiro+" a "+ultimo+"). Primeiro: "+numeros(dados.get(0).mask)+" / "+MESES[dados.get(0).mes-1]+". Último: "+numeros(dados.get(dados.size()-1).mask)+" / "+MESES[dados.get(dados.size()-1).mes-1]+". Toque em ANALISAR.");
  }
  static String numeros(int mask){StringBuilder sb=new StringBuilder();for(int n=1;n<=31;n++)if((mask&(1<<(n-1)))!=0)sb.append(String.format(Locale.US,"%02d ",n));return sb.toString().trim();}
  static int mascaraTiras(int subset){int mask=0;for(int t=0;t<8;t++)if((subset&(1<<t))!=0)for(int n=t*4+1;n<=Math.min(31,t*4+4);n++)mask|=1<<(n-1);return mask;}
  static String nomesTiras(int subset){StringBuilder s=new StringBuilder();for(int t=0;t<8;t++)if((subset&(1<<t))!=0){if(s.length()>0)s.append("+");s.append("T").append(t+1);}return s.toString();}
  void analisar(){if(dados.isEmpty()){dizer("Selecione primeiro o histórico.");return;}todas.clear();candidatas.clear();
    for(int subset=1;subset<256;subset++){Stat st=new Stat();st.tiras=subset;st.mask=mascaraTiras(subset);st.qtd=Integer.bitCount(st.mask);int run=0,ultimoIdx=-1;
      for(int i=0;i<dados.size();i++){if((st.mask&dados.get(i).mask)==0){st.falhas++;ultimoIdx=i;run++;st.maiorSeq=Math.max(st.maiorSeq,run);}else run=0;}
      st.atraso=ultimoIdx<0?dados.size():dados.size()-1-ultimoIdx;st.ultimo=ultimoIdx<0?0:dados.get(ultimoIdx).concurso;todas.add(st);
      if(Integer.bitCount(subset)==1||Integer.bitCount(subset)==2)candidatas.add(st);
    }
    // Reproduzir a ordem inicial de itertools.combinations do Python V5.
    // Java sort e estavel: em empate total, preservar 1 tirinha, depois pares lexicograficos.
    candidatas.sort((a,b)->{
      int c=Integer.compare(Integer.bitCount(a.tiras),Integer.bitCount(b.tiras));if(c!=0)return c;
      int[] aa=new int[2],bb=new int[2];int ia=0,ib=0;
      for(int t=0;t<8;t++){if((a.tiras&(1<<t))!=0)aa[ia++]=t;if((b.tiras&(1<<t))!=0)bb[ib++]=t;}
      for(int i=0;i<ia;i++){c=Integer.compare(aa[i],bb[i]);if(c!=0)return c;}return 0;
    });
    // V5: ordenacao por atraso decrescente; desempate taxa de falhas decrescente.
    candidatas.sort((a,b)->{int c=Integer.compare(b.atraso,a.atraso);return c!=0?c:Integer.compare(b.falhas,a.falhas);});
    indiceCandidato=0;analisado=true;
    dizer("Analisadas "+todas.size()+" combinações em "+dados.size()+" concursos. Seleção V5: somente 1 ou 2 tirinhas; 20 opções mais atrasadas. Toque em ESCOLHER TIRINHAS.");
  }
  void escolherTiras(){if(!analisado){dizer("Analise o histórico primeiro.");return;}String[] itens=new String[21];itens[0]="0 - NÃO RETIRAR TIRINHAS";
    for(int i=0;i<20;i++){Stat s=candidatas.get(i);itens[i+1]=(i+1)+" - "+nomesTiras(s.tiras)+" | "+s.qtd+" dz | falhas "+s.falhas+" | atraso "+s.atraso+" | "+numeros(s.mask);}
    new AlertDialog.Builder(this).setTitle("V5 - Escolha 0 a 20").setItems(itens,(d,pos)->{retiradasMask=pos==0?0:candidatas.get(pos-1).mask;jogo=new int[0];mes=0;dizer("TIRINHAS PARA FALHA: "+(pos==0?"nenhuma":numeros(retiradasMask))+". Agora gere o jogo.");}).show();
  }
  void gerarJogo(){if(!analisado){dizer("Analise o histórico e escolha as tirinhas.");return;}
    // PORT EXATO DA V5: ordenar dezenas DISPONIVEIS por frequencia decrescente
    // nos ultimos 100 concursos; empate pelo numero crescente; maximo 4 de cada paridade.
    int[] freq=new int[32];for(int i=Math.max(0,dados.size()-100);i<dados.size();i++)for(int n=1;n<=31;n++)if((dados.get(i).mask&(1<<(n-1)))!=0)freq[n]++;
    ArrayList<Integer> disponiveis=new ArrayList<>();for(int n=1;n<=31;n++)if((retiradasMask&(1<<(n-1)))==0)disponiveis.add(n);
    disponiveis.sort((a,b)->{int c=Integer.compare(freq[b],freq[a]);return c!=0?c:Integer.compare(a,b);});
    ArrayList<Integer> selecao=new ArrayList<>();for(int n:disponiveis){if(selecao.size()>=7)break;int paridade=0;for(int j:selecao)if(j%2==n%2)paridade++;if(paridade<4)selecao.add(n);}
    for(int n:disponiveis){if(selecao.size()>=7)break;if(!selecao.contains(n))selecao.add(n);}
    if(selecao.size()!=7){dizer("Não existem 7 dezenas disponíveis.");return;}
    jogo=new int[7];for(int i=0;i<7;i++)jogo[i]=selecao.get(i);Arrays.sort(jogo);
    String[] mesesOrdenados=new String[12];int[] mesNumeros=new int[12];int[] mf=new int[13],atraso=new int[13];Arrays.fill(atraso,dados.size());
    int pos=0;for(int i=Math.max(0,dados.size()-100);i<dados.size();i++)mf[dados.get(i).mes]++;
    for(int i=dados.size()-1;i>=0;i--){int m=dados.get(i).mes;if(atraso[m]==dados.size())atraso[m]=pos;pos++;}
    ArrayList<Integer> ordem=new ArrayList<>();for(int m=1;m<=12;m++)ordem.add(m);
    ordem.sort((a,b)->{int c=Integer.compare(mf[a],mf[b]);if(c!=0)return c;c=Integer.compare(atraso[b],atraso[a]);return c!=0?c:Integer.compare(a,b);});
    mes=ordem.get(0);
    for(int i=0;i<12;i++){int m=ordem.get(i);mesNumeros[i]=m;mesesOrdenados[i]=MESES[m-1]+" | últimos 100: "+mf[m]+" | atraso: "+atraso[m];}
    new AlertDialog.Builder(this).setTitle("JOGO V5: "+jogoTexto()+"\nEscolha o Mês da Sorte").setSingleChoiceItems(mesesOrdenados,0,(d,which)->{mes=mesNumeros[which];d.dismiss();confirmarJogo();}).setPositiveButton("ACEITAR MÊS SUGERIDO",(d,w)->confirmarJogo()).show();
  }
  String jogoTexto(){StringBuilder b=new StringBuilder();for(int n:jogo)b.append(String.format(Locale.US,"%02d ",n));return b.toString().trim();}
  void confirmarJogo(){new AlertDialog.Builder(this).setTitle("CONFERIR JOGO V7 / MOTOR V5").setMessage("7 DEZENAS: "+jogoTexto()+"\nMÊS: "+MESES[mes-1]+"\nTIRINHAS RETIRADAS: "+(retiradasMask==0?"nenhuma":numeros(retiradasMask))+"\n\nEste resultado segue o algoritmo da V5 Windows.").setPositiveButton("CONFIRMAR",(d,w)->dizer("JOGO: "+jogoTexto()+"\nMÊS DA SORTE: "+MESES[mes-1]+"\nPronto para salvar o PDF colorido.")).setNegativeButton("VOLTAR",null).show();}
  void salvarPdf(){if(jogo.length!=7||mes==0){dizer("Gere o jogo e confirme o Mês da Sorte primeiro.");return;}Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");i.putExtra(Intent.EXTRA_TITLE,"DIA_DE_SORTE_CAMPEAO_V7_VOLANTE.pdf");startActivityForResult(i,SAVE);}
  static void linha(Canvas c,Paint p,String t,float x,float y,int tamanho,boolean negrito){p.setColor(ESCURO);p.setTextSize(tamanho);p.setTypeface(Typeface.create("sans-serif",negrito?Typeface.BOLD:Typeface.NORMAL));c.drawText(t,x,y,p);}
  void pdf(Uri uri)throws Exception{
    PdfDocument doc=new PdfDocument();int largura=595,altura=842;
    PdfDocument.Page pagina=doc.startPage(new PdfDocument.PageInfo.Builder(largura,altura,1).create());Canvas c=pagina.getCanvas();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    p.setColor(Color.rgb(245,183,18));c.drawRect(0,0,595,100,p);linha(c,p,"DIA DE SORTE CAMPEÃO V7",40,48,22,true);linha(c,p,"VOLANTE COLORIDO - MODELO PARA ESTUDO",40,77,11,false);
    linha(c,p,"7 DEZENAS MARCADAS",43,133,16,true);HashSet<Integer> marcadas=new HashSet<>();for(int n:jogo)marcadas.add(n);
    for(int n=1;n<=31;n++){int idx=n-1,row=idx/7,col=idx%7;float x=45+col*73,y=158+row*59;
      p.setColor(marcadas.contains(n)?AMARELO:Color.rgb(242,242,242));c.drawRoundRect(x,y,x+55,y+48,7,7,p);
      p.setColor(marcadas.contains(n)?Color.rgb(185,130,0):Color.rgb(205,205,205));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);c.drawRoundRect(x,y,x+55,y+48,7,7,p);p.setStyle(Paint.Style.FILL);
      linha(c,p,String.format(Locale.US,"%02d",n),x+13,y+31,18,marcadas.contains(n));
    }
    linha(c,p,"MÊS DA SORTE",43,508,15,true);
    for(int i=0;i<12;i++){int row=i/4,col=i%4;float x=43+col*129,y=522+row*37;p.setColor(i+1==mes?AMARELO:Color.rgb(243,243,243));c.drawRoundRect(x,y,x+117,y+30,6,6,p);linha(c,p,MESES[i],x+7,y+20,10,i+1==mes);}
    linha(c,p,"JOGO: "+jogoTexto(),43,666,13,true);linha(c,p,"TIRINHAS RETIRADAS: "+(retiradasMask==0?"nenhuma":numeros(retiradasMask)),43,695,10,false);
    linha(c,p,"HISTÓRICO: "+dados.size()+" concursos | "+dados.get(0).concurso+" a "+dados.get(dados.size()-1).concurso,43,723,10,false);
    linha(c,p,"Modelo para estudo - não é comprovante de aposta.",43,756,10,false);
    linha(c,p,"Atrasos e falhas históricas não garantem resultados futuros.",43,775,9,false);
    doc.finishPage(pagina);
    ArrayList<Stat> ordenadas=new ArrayList<>(todas);ordenadas.sort((a,b)->{int x=Integer.compare(a.qtd,b.qtd);if(x!=0)return x;x=Integer.compare(b.atraso,a.atraso);return x!=0?x:Integer.compare(b.falhas,a.falhas);});
    int paginaN=2,linhaN=0;Canvas rc=null;
    for(Stat s:ordenadas){if(linhaN==0){PdfDocument.Page pg=doc.startPage(new PdfDocument.PageInfo.Builder(largura,altura,paginaN++).create());rc=pg.getCanvas();p.setColor(AMARELO);rc.drawRect(0,0,595,65,p);linha(rc,p,"RANKING V5 - 255 COMBINAÇÕES",30,43,17,true);pagina=pg;}
      String t=nomesTiras(s.tiras)+" | "+s.qtd+" dz | falhas "+s.falhas+" | atraso "+s.atraso+" | seq "+s.maiorSeq;
      linha(rc,p,t,30,91+linhaN*17,9,false);linhaN++;
      if(linhaN>=41){doc.finishPage(pagina);linhaN=0;}
    }
    if(linhaN>0)doc.finishPage(pagina);
    try(OutputStream out=getContentResolver().openOutputStream(uri,"w")){if(out==null)throw new IOException("Destino não disponível");doc.writeTo(out);out.flush();}finally{doc.close();}
    dizer("PDF COLORIDO V7 SALVO. Arquivo: DIA_DE_SORTE_CAMPEAO_V7_VOLANTE.pdf. Confira a pasta selecionada no Android.");
  }
}
