import unittest, io, json, os
from unittest.mock import patch
from ai_server import validate, generate
class TestAI(unittest.TestCase):
 def test_invalid_source(self):
  for value in ('', 'x'*30001, None):
   with self.assertRaises(ValueError):validate({'source':value,'question':'설명'})
 def test_unconfigured_is_error(self):
  with patch.dict(os.environ,{},clear=True):
   with self.assertRaises(RuntimeError):generate({'source':'자료','question':'설명'})
 def test_real_provider_contract_with_mock_transport(self):
  captured=[]
  def upstream(req,timeout):
   captured.append(req)
   return io.BytesIO(json.dumps({'candidates':[{'content':{'parts':[{'text':'테스트 설명'}]}}]}).encode())
  with patch.dict(os.environ,{'GEMINI_API_KEY':'test-key','GEMINI_MODEL':'test-model'}):
   out=generate({'source':'내 자료','question':'요약'},opener=upstream)
  self.assertTrue(out['generated']);self.assertEqual(out['answer'],'테스트 설명')
  self.assertIn('내 자료',json.loads(captured[0].data)['contents'][0]['parts'][0]['text'])
  self.assertNotIn('test-key',captured[0].full_url)
if __name__=='__main__':unittest.main()
